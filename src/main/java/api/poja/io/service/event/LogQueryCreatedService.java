package api.poja.io.service.event;

import static api.poja.io.endpoint.rest.model.FunctionType.FRONTAL;
import static api.poja.io.endpoint.rest.model.FunctionType.WORKER;

import api.poja.io.aws.cloudwatch.CloudwatchComponent;
import api.poja.io.endpoint.event.EventProducer;
import api.poja.io.endpoint.event.model.GetLogQueryResultRequested;
import api.poja.io.endpoint.event.model.LogQueryCreated;
import api.poja.io.endpoint.rest.model.EnvironmentType;
import api.poja.io.endpoint.rest.model.FunctionType;
import api.poja.io.repository.model.ComputeStackResource;
import api.poja.io.repository.model.LogQuery;
import api.poja.io.service.ComputeStackResourceService;
import api.poja.io.service.logQuery.LogQueryService;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import org.apache.commons.text.StringEscapeUtils;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.cloudwatchlogs.model.DescribeLogGroupsResponse;
import software.amazon.awssdk.services.cloudwatchlogs.model.LogGroup;

@Service
@AllArgsConstructor
public class LogQueryCreatedService implements Consumer<LogQueryCreated> {
  private final CloudwatchComponent cloudwatchComponent;
  private final LogQueryService logQueryService;
  private final EventProducer<GetLogQueryResultRequested> eventProducer;
  private final ComputeStackResourceService computeStackResourceService;
  private static final String LAMBDA_LOG_GROUP_NAME_PREFIX = "/aws/lambda/%s";
  private static final String BASIC_LOG_INSIGHTS_QUERY =
      """
      fields @timestamp, @message, @logStream, @log
      | %s
      | sort @timestamp desc
      | limit 10000
      """;

  @Override
  public void accept(LogQueryCreated logQueryCreated) {
    String domainQueryId = logQueryCreated.getDomainQueryId();
    LogQuery domain = logQueryService.getById(domainQueryId);
    var queryId =
        cloudwatchComponent.initiateLogInsightsQuery(
            BASIC_LOG_INSIGHTS_QUERY.formatted(
                getFilters(logQueryCreated.getQueryFilterKeywords())),
            logQueryCreated.getQueryInstantIntervalBegin(),
            logQueryCreated.getQueryInstantIntervalEnd(),
            getLogGroups(
                domain.getAppId(),
                logQueryCreated.getEnvironmentTypes(),
                logQueryCreated.getFunctionTypes()));
    logQueryService.updateQueryId(domainQueryId, queryId);
    eventProducer.accept(
        List.of(
            GetLogQueryResultRequested.builder()
                .domainQueryId(domainQueryId)
                .queryId(queryId)
                .build()));
  }

  private List<String> getLogGroups(
      String appId, Set<EnvironmentType> envTypes, Set<FunctionType> functionTypes) {
    List<ComputeStackResource> stackResources =
        computeStackResourceService.findLatestResourcesByEnvTypes(appId, envTypes);
    return stackResources.stream()
        .flatMap(
            stackResource -> {
              List<String> logGroups = new ArrayList<>();
              if (functionTypes.contains(FRONTAL)
                  && stackResource.getFrontalFunctionName() != null) {
                logGroups.add(
                    String.format(
                        LAMBDA_LOG_GROUP_NAME_PREFIX, stackResource.getFrontalFunctionName()));
              }
              if (functionTypes.contains(WORKER)) {
                for (var workerFunction : stackResource.getWorkerFunctions()) {
                  logGroups.add(
                      String.format(LAMBDA_LOG_GROUP_NAME_PREFIX, workerFunction.getName()));
                }
              }
              return logGroups.stream();
            })
        .map(this::getAllLogGroups)
        .flatMap(List::stream)
        .map(LogGroup::logGroupName)
        .distinct()
        .toList();
  }

  private List<LogGroup> getAllLogGroups(String namePattern) {
    var logGroupsIterator =
        cloudwatchComponent.getLambdaFunctionLogGroupsByNamePatternIterator(namePattern);
    var logGroups = new ArrayList<LogGroup>();
    while (logGroupsIterator.hasNext()) {
      DescribeLogGroupsResponse current = logGroupsIterator.next();
      logGroups.addAll(current.logGroups());
    }
    return logGroups;
  }

  private static String getFilters(Set<String> filterKeywords) {
    List<String> escapedKeywords =
        filterKeywords.stream().map(StringEscapeUtils::escapeJson).toList();
    StringBuilder filter = new StringBuilder("filter ");

    for (int i = 0; i < escapedKeywords.size(); i++) {
      filter.append("@message LIKE \"").append(escapedKeywords.get(i)).append("\"");
      if (i < escapedKeywords.size() - 1) {
        filter.append(" AND ");
      }
    }

    return filter.toString();
  }
}
