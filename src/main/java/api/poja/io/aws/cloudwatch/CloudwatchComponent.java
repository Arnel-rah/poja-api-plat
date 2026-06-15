package api.poja.io.aws.cloudwatch;

import static java.nio.charset.StandardCharsets.UTF_8;

import api.poja.io.aws.AwsConf;
import api.poja.io.model.exception.InternalServerErrorException;
import java.net.URI;
import java.net.URLEncoder;
import java.time.Instant;
import java.util.Iterator;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClient;
import software.amazon.awssdk.services.cloudwatchlogs.model.DescribeLogGroupsRequest;
import software.amazon.awssdk.services.cloudwatchlogs.model.DescribeLogGroupsResponse;
import software.amazon.awssdk.services.cloudwatchlogs.model.GetQueryResultsResponse;
import software.amazon.awssdk.services.cloudwatchlogs.model.LogGroup;
import software.amazon.awssdk.services.cloudwatchlogs.paginators.DescribeLogGroupsIterable;

@Component
@AllArgsConstructor
@Slf4j
public class CloudwatchComponent {
  private static final String AWS_LOG_GROUPS_ALL_EVENTS_LINK_TEMPLATE =
      "https://%s.console.aws.amazon.com/cloudwatch/home?region=%s#logsV2:log-groups/log-group/%s/log-events";
  public static final String LOG_EVENTS_WITH_START_TIME_LINK_TEMPLATE =
      "https://%s.console.aws.amazon.com/cloudwatch/home?region=%s#logsV2:log-groups/log-group/%s/log-events/%s?start=%s";
  private final CloudWatchLogsClient cloudWatchLogsClient;
  private final AwsConf awsConf;

  public List<LogGroup> getLambdaFunctionLogGroupsByNamePattern(String namePattern) {
    DescribeLogGroupsRequest request =
        DescribeLogGroupsRequest.builder().logGroupNamePattern(namePattern).build();
    try {
      DescribeLogGroupsResponse response = cloudWatchLogsClient.describeLogGroups(request);
      return response.logGroups();
    } catch (AwsServiceException | SdkClientException e) {
      log.error("Error occurred when retrieving log groups of name pattern={}", namePattern);
      throw new InternalServerErrorException(e);
    }
  }

  public Iterator<DescribeLogGroupsResponse> getLambdaFunctionLogGroupsByNamePatternIterator(
      String namePattern) {
    try {
      log.info("fetching logs for {}", namePattern);
      DescribeLogGroupsIterable response =
          cloudWatchLogsClient.describeLogGroupsPaginator(
              req -> req.logGroupNamePattern(namePattern));
      return response.iterator();
    } catch (AwsServiceException | SdkClientException e) {
      log.error("Error occurred when retrieving log groups of name pattern={}", namePattern);
      throw new InternalServerErrorException(e);
    }
  }

  public String initiateLogInsightsQuery(
      String queryString, Instant startTime, Instant endTime, List<String> logGroupNames) {
    if (logGroupNames.size() > 50) {
      throw new InternalServerErrorException(
          "cannot start insights query with more than 50 log group names");
    }
    log.info("querying with {} {}", logGroupNames.size(), logGroupNames);
    var query =
        cloudWatchLogsClient.startQuery(
            sqr ->
                sqr.queryString(queryString)
                    .startTime(startTime.getEpochSecond())
                    .endTime(endTime.getEpochSecond())
                    .logGroupNames(logGroupNames));
    return query.queryId();
  }

  public GetQueryResultsResponse getQueryResult(String queryId) {
    return cloudWatchLogsClient.getQueryResults(gqr -> gqr.queryId(queryId));
  }

  public URI getLogGroupAllEventsUri(String logGroupName) {
    return URI.create(
        AWS_LOG_GROUPS_ALL_EVENTS_LINK_TEMPLATE.formatted(
            awsConf.getRegion(), awsConf.getRegion(), URLEncoder.encode(logGroupName, UTF_8)));
  }

  public URI computeLogUri(String logGroup, String logStream, Instant timestamp) {
    String region = awsConf.getRegion().toString();
    String encodedLogGroup = URLEncoder.encode(logGroup, UTF_8);
    String encodedLogStream = URLEncoder.encode(logStream, UTF_8);
    String encodedTimestamp = URLEncoder.encode(timestamp.toString(), UTF_8);
    String url =
        String.format(
            LOG_EVENTS_WITH_START_TIME_LINK_TEMPLATE,
            region,
            region,
            encodedLogGroup,
            encodedLogStream,
            encodedTimestamp);
    return URI.create(url);
  }
}
