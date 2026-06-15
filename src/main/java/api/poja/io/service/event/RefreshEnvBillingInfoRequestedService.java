package api.poja.io.service.event;

import static api.poja.io.repository.model.enums.BillingInfoComputeStatus.FINISHED;
import static api.poja.io.repository.model.enums.BillingInfoComputeStatus.PENDING;
import static java.math.BigDecimal.ZERO;

import api.poja.io.aws.cloudwatch.CloudwatchComponent;
import api.poja.io.endpoint.event.EventProducer;
import api.poja.io.endpoint.event.model.RefreshEnvBillingInfoRequested;
import api.poja.io.endpoint.event.model.UserEnvBillingInfoUpdateFromQueryRequested;
import api.poja.io.repository.model.Application;
import api.poja.io.repository.model.BillingInfo;
import api.poja.io.repository.model.Environment;
import api.poja.io.service.ApplicationService;
import api.poja.io.service.BillingInfoService;
import api.poja.io.service.EnvironmentService;
import api.poja.io.service.UserService;
import api.poja.io.sys.platform.SaasOnly;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.cloudwatchlogs.model.DescribeLogGroupsResponse;

@Component
@AllArgsConstructor
@Slf4j
@SaasOnly
public class RefreshEnvBillingInfoRequestedService
    implements Consumer<RefreshEnvBillingInfoRequested> {
  private static final String LOG_INSIGHTS_QUERY_TOTAL_MEMORY_DURATION =
      """

fields @timestamp, @billedDuration/60000 as durationInMinutes, @memorySize/(1000000) as memorySizeInMo
 | filter @message like /REPORT RequestId:/
 | stats sum(durationInMinutes * memorySizeInMo) as billedMemoryDurationGrouped, sum(durationInMinutes) as billedDurationGrouped by memorySizeInMo
 | stats sum(billedMemoryDurationGrouped) as billedMemoryDuration, sum(billedDurationGrouped) as computedBilledDuration
""";
  private static final String LOG_GROUP_NAME_PATTERN_FOR_FRONTAL_FUNCTION_1 =
      "/aws/lambda/%s-compute-%s-FrontalFunction";
  private static final String LOG_GROUP_NAME_PATTERN_FOR_WORKER_FUNCTIONS_1 =
      "/aws/lambda/%s-compute-%s-WorkerFunction";
  private static final String LOG_GROUP_NAME_PATTERN_FOR_FRONTAL_FUNCTION_2 =
      "/aws/lambda/%s-%s-FrontalFunction";
  private static final String LOG_GROUP_NAME_PATTERN_FOR_WORKER_FUNCTIONS_2 =
      "/aws/lambda/%s-%s-WorkerFunction";
  private final CloudwatchComponent cloudwatchComponent;
  private final ApplicationService applicationService;
  private final EnvironmentService environmentService;
  private final BillingInfoService billingInfoService;
  private final UserService userService;
  private final EventProducer<UserEnvBillingInfoUpdateFromQueryRequested> eventProducer;

  @Override
  public void accept(RefreshEnvBillingInfoRequested event) {
    var eventId = event.getId().toString();
    var app = applicationService.getById(event.getAppId());
    var env = environmentService.getById(event.getEnvId());

    log.info(
        "RefreshEnvBillingInfoRequested for org {}, user {}, app {}, env {}",
        event.getOrgId(),
        event.getUserId(),
        event.getAppId(),
        event.getEnvId());

    var billingInfo = billingInfoService.findBy(app.getId(), env.getId(), eventId);
    Instant startTime = event.getPricingCalculationRequestStartTime();
    Instant endTime = event.getPricingCalculationRequestEndTime();
    if (billingInfo.isPresent()) {
      String queryId = billingInfo.get().getQueryId();
      if (queryId != null) {
        log.info("retrying already existing query {}", queryId);
        saveBillingInfo(eventId, app, queryId, env, endTime);
        fireUserBillingUpdateEvent(event, queryId, app, env);
        return;
      }
    }
    var logGroups = getAllLogGroups(app, env);
    if (logGroups.isEmpty()) {
      log.info("empty log groups");
      billingInfoService.crupdateBillingInfo(
          BillingInfo.builder()
              .id(eventId)
              .userId(event.getUserId())
              .computationIntervalEnd(event.getPricingCalculationRequestEndTime())
              .computeDatetime(Instant.now())
              .computedPrice(ZERO)
              .computedMemoryDurationInMbMinutes(ZERO)
              .appId(event.getAppId())
              .orgId(event.getOrgId())
              .envId(event.getEnvId())
              .queryId(null)
              .status(FINISHED)
              .pricingMethod(event.getPricingMethod())
              .build());
      return;
    }
    var queryId = initiateQuery(startTime, endTime, logGroups);
    log.info("query {} initiated", queryId);
    saveBillingInfo(eventId, app, queryId, env, endTime);
    fireUserBillingUpdateEvent(event, queryId, app, env);
  }

  private void fireUserBillingUpdateEvent(
      RefreshEnvBillingInfoRequested event, String queryId, Application app, Environment env) {
    eventProducer.accept(
        List.of(
            new UserEnvBillingInfoUpdateFromQueryRequested(
                queryId, app.getUserId(), env.getId(), event)));
  }

  private String initiateQuery(Instant startTime, Instant endTime, List<DomainLogGroup> logGroups) {
    if (logGroups.size() == 1) {
      var lg = logGroups.getFirst();
      return cloudwatchComponent.initiateLogInsightsQuery(
          LOG_INSIGHTS_QUERY_TOTAL_MEMORY_DURATION,
          lg.creationDatetime.isAfter(startTime) ? lg.creationDatetime : startTime,
          endTime,
          logGroups.stream().map(DomainLogGroup::name).toList());
    }
    return cloudwatchComponent.initiateLogInsightsQuery(
        LOG_INSIGHTS_QUERY_TOTAL_MEMORY_DURATION,
        startTime,
        endTime,
        logGroups.stream().map(DomainLogGroup::name).toList());
  }

  private void saveBillingInfo(
      String eventId,
      Application app,
      String queryId,
      Environment env,
      Instant computationRequestEndDatetime) {
    var user = userService.getUserById(app.getUserId());
    var billingInfoToSave =
        BillingInfo.builder()
            .id(eventId)
            .queryId(queryId)
            .userId(user.getId())
            .orgId(app.getOrgId())
            .appId(app.getId())
            .envId(env.getId())
            .computationIntervalEnd(computationRequestEndDatetime)
            .pricingMethod(user.getPricingMethod())
            .status(PENDING)
            .build();
    billingInfoService.crupdateBillingInfo(billingInfoToSave);
  }

  private static String formatEnvTypeToLogGroupPattern(Environment env) {
    return env.getEnvironmentType().name().toLowerCase();
  }

  private static String frontalFunctionLogGroupNamePattern1(String appName, Environment env) {
    return LOG_GROUP_NAME_PATTERN_FOR_FRONTAL_FUNCTION_1.formatted(
        formatEnvTypeToLogGroupPattern(env), appName);
  }

  private static String workerFunctionLogGroupNamePattern1(String appName, Environment env) {
    return LOG_GROUP_NAME_PATTERN_FOR_WORKER_FUNCTIONS_1.formatted(
        formatEnvTypeToLogGroupPattern(env), appName);
  }

  private static String frontalFunctionLogGroupNamePattern2(String appName, Environment env) {
    return LOG_GROUP_NAME_PATTERN_FOR_FRONTAL_FUNCTION_2.formatted(
        formatEnvTypeToLogGroupPattern(env), appName);
  }

  private static String workerFunctionLogGroupNamePattern2(String appName, Environment env) {
    return LOG_GROUP_NAME_PATTERN_FOR_WORKER_FUNCTIONS_2.formatted(
        formatEnvTypeToLogGroupPattern(env), appName);
  }

  private List<DomainLogGroup> getAllLogGroups(Application app, Environment env) {
    // Note: Newer applications are not billed because their environment log groups are not queried.
    // Older applications used an unsuffixed Application.name, so their log groups are still billed.
    var legacyAppName = app.getFormattedName();
    var newAppName = app.getSuffixedAppName();

    // TODO: refactor
    var legacyRawFrontalLogGroups1 =
        getAllLogGroups(frontalFunctionLogGroupNamePattern1(legacyAppName, env));
    var legacyRawWorkerLogGroups1 =
        getAllLogGroups(workerFunctionLogGroupNamePattern1(legacyAppName, env));
    var legacyRawFrontalLogGroups2 =
        getAllLogGroups(frontalFunctionLogGroupNamePattern2(legacyAppName, env));
    var legacyRawWorkerLogGroups2 =
        getAllLogGroups(workerFunctionLogGroupNamePattern2(legacyAppName, env));

    var rawFrontalLogGroups1 =
        getAllLogGroups(frontalFunctionLogGroupNamePattern1(newAppName, env));
    var rawWorkerLogGroups1 = getAllLogGroups(workerFunctionLogGroupNamePattern1(newAppName, env));
    var rawFrontalLogGroups2 =
        getAllLogGroups(frontalFunctionLogGroupNamePattern2(newAppName, env));
    var rawWorkerLogGroups2 = getAllLogGroups(workerFunctionLogGroupNamePattern2(newAppName, env));

    return mergeListsAndExtractLogGroupNames(
        rawFrontalLogGroups1,
        rawWorkerLogGroups1,
        legacyRawFrontalLogGroups1,
        legacyRawWorkerLogGroups1,
        rawFrontalLogGroups2,
        rawWorkerLogGroups2,
        legacyRawFrontalLogGroups2,
        legacyRawWorkerLogGroups2);
  }

  private List<DomainLogGroup> getAllLogGroups(String namePattern) {
    var logGroupsIterator =
        cloudwatchComponent.getLambdaFunctionLogGroupsByNamePatternIterator(namePattern);
    var logGroups = new ArrayList<DomainLogGroup>();
    while (logGroupsIterator.hasNext()) {
      DescribeLogGroupsResponse current = logGroupsIterator.next();
      logGroups.addAll(
          current.logGroups().stream()
              .map(
                  lg ->
                      new DomainLogGroup(
                          lg.logGroupName(), Instant.ofEpochMilli(lg.creationTime())))
              .toList());
    }
    return logGroups;
  }

  @SafeVarargs
  private List<DomainLogGroup> mergeListsAndExtractLogGroupNames(
      List<DomainLogGroup>... logGroups) {
    return Stream.of(logGroups).flatMap(List::stream).toList();
  }

  private record DomainLogGroup(String name, Instant creationDatetime) {}
}
