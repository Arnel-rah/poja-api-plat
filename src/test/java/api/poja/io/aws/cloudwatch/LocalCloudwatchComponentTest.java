package api.poja.io.aws.cloudwatch;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Comparator.comparing;
import static software.amazon.awssdk.regions.Region.EU_WEST_3;

import api.poja.io.aws.AwsConf;
import api.poja.io.endpoint.rest.model.EnvFunctionLog;
import api.poja.io.model.exception.NotFoundException;
import java.net.URLEncoder;
import java.time.Instant;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClient;
import software.amazon.awssdk.services.cloudwatchlogs.model.GetQueryResultsResponse;
import software.amazon.awssdk.services.cloudwatchlogs.model.LogGroup;
import software.amazon.awssdk.services.cloudwatchlogs.model.QueryStatus;
import software.amazon.awssdk.services.cloudwatchlogs.model.ResultField;

@Disabled("run in local only")
@Slf4j
class LocalCloudwatchComponentTest {
  // Replace with /poja-app-deployer/${Env}/target-account/execution/role-arn
  String executionRoleArn = "";
  ProfileCredentialsProvider credentialsProvider = ProfileCredentialsProvider.create("jc-prod");
  CloudwatchComponent cloudwatchComponent =
      new CloudwatchComponent(
          CloudWatchLogsClient.builder()
              .region(EU_WEST_3)
              .credentialsProvider(credentialsProvider)
              .build(),
          new AwsConf("id", EU_WEST_3, executionRoleArn));

  @Test
  void initiateLogInsightsQuery() {
    var beginTestTime = System.currentTimeMillis();
    Instant startTime = Instant.parse("2025-02-10T00:00:00Z");
    Instant endTime = Instant.parse("2025-02-10T07:40:59Z");
    String queryId =
        cloudwatchComponent.initiateLogInsightsQuery(
            """
fields @timestamp, @message, @logStream, @log
| sort @timestamp desc
| limit 10000
""",
            startTime,
            endTime,
            List.of("/aws/lambda/prod-compute-jcloudify-api-FrontalFunction-g8DDzD8v9Nhy"));

    // Poll for query completion
    boolean isQueryComplete = false;
    while (!isQueryComplete) {
      try {
        Thread.sleep(1000); // Sleep for 1 second before checking again
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        e.printStackTrace();
      }

      GetQueryResultsResponse getQueryResultsResponse = cloudwatchComponent.getQueryResult(queryId);
      QueryStatus status = getQueryResultsResponse.status();

      if (status == QueryStatus.COMPLETE) {
        isQueryComplete = true;
        long testEndTime = System.currentTimeMillis(); // End timing
        long duration = testEndTime - beginTestTime;
        List<List<ResultField>> results = getQueryResultsResponse.results();
        log.info("results {}", results);
      } else if (status == QueryStatus.FAILED) {
        log.info("Query failed.");
        break;
      }
    }
  }

  @Test
  void test() {
    String functionName = "preprod-compute-moulfate-FrontalFunction-7KnCuu5o2lqj";
    var existingLogs = cloudwatchComponent.getLambdaFunctionLogGroupsByNamePattern(functionName);
    log.info("existingLogs = {}", existingLogs);
    if (existingLogs.isEmpty()) {
      throw new NotFoundException("No logs found for function name: " + functionName);
    }
    var filteredLogGroups =
        existingLogs.stream()
            .filter(l -> l.logGroupName().endsWith(functionName))
            .sorted(comparing(LogGroup::creationTime).reversed())
            .toList();
    if (filteredLogGroups.isEmpty()) {
      throw new NotFoundException("No logs found for function name: " + functionName);
    }
    LogGroup latestLogGroup = filteredLogGroups.getFirst();
    String logGroupName = latestLogGroup.logGroupName();
    new EnvFunctionLog()
        .name(logGroupName)
        .creationDatetime(Instant.ofEpochMilli(latestLogGroup.creationTime()))
        .link(cloudwatchComponent.getLogGroupAllEventsUri(logGroupName));
  }

  @Test
  void get_link() {
    String region = "eu-west-3"; // Update with your AWS region
    String logGroup =
        "/aws/lambda/prod-compute-jcloudify-api-FrontalFunction-g8DDzD8v9Nhy"; // Extracted from
    // @log
    String logStream =
        "2025/02/10/[121]f53573d8640343feb04aca5051545fc8"; // Extracted from @logStream
    String timestamp = "2025-02-10T07:35:47.240Z"; // Extracted from @timestamp

    String encodedLogGroup = URLEncoder.encode(logGroup, UTF_8);
    String encodedLogStream = URLEncoder.encode(logStream, UTF_8);
    String encodedTimestamp = URLEncoder.encode(timestamp, UTF_8);

    // Construct URL
    String cloudWatchUrl =
        String.format(
            "https://%s.console.aws.amazon.com/cloudwatch/home?region=%s#logsV2:log-groups/log-group/%s/log-events/%s?start=%s",
            region, region, encodedLogGroup, encodedLogStream, encodedTimestamp);

    log.info("CloudWatch Log Stream URL: {}", cloudWatchUrl);
  }
}
