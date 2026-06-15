package api.poja.io.aws.cloudwatch;

import static java.util.Collections.emptyIterator;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import api.poja.io.conf.MockedThirdParties;
import api.poja.io.model.exception.InternalServerErrorException;
import java.net.URI;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClient;
import software.amazon.awssdk.services.cloudwatchlogs.model.DescribeLogGroupsRequest;
import software.amazon.awssdk.services.cloudwatchlogs.model.DescribeLogGroupsResponse;
import software.amazon.awssdk.services.cloudwatchlogs.model.GetQueryResultsResponse;
import software.amazon.awssdk.services.cloudwatchlogs.model.StartQueryResponse;
import software.amazon.awssdk.services.cloudwatchlogs.paginators.DescribeLogGroupsIterable;

class CloudwatchComponentTest extends MockedThirdParties {
  @MockBean CloudWatchLogsClient cloudWatchLogsClientMock;
  @Autowired CloudwatchComponent subject;

  @Test
  void getLambdaFunctionLogGroupsByNamePattern() {
    var describeLogRequest = DescribeLogGroupsRequest.builder().logGroupNamePattern("any").build();
    var failingDescribeLogRequest =
        DescribeLogGroupsRequest.builder().logGroupNamePattern("failing").build();
    DescribeLogGroupsResponse expectedResponse =
        DescribeLogGroupsResponse.builder().logGroups(List.of()).build();
    when(cloudWatchLogsClientMock.describeLogGroups(eq(describeLogRequest)))
        .thenReturn(expectedResponse);
    when(cloudWatchLogsClientMock.describeLogGroups(eq(failingDescribeLogRequest)))
        .thenThrow(AwsServiceException.class);

    assertThrows(
        InternalServerErrorException.class,
        () -> subject.getLambdaFunctionLogGroupsByNamePattern("failing"));
    assertEquals(
        expectedResponse.logGroups(), subject.getLambdaFunctionLogGroupsByNamePattern("any"));
  }

  @Test
  void getLambdaFunctionLogGroupsByNamePatternIterator_ok() {
    DescribeLogGroupsIterable expectedResponse = mock(DescribeLogGroupsIterable.class);
    when(expectedResponse.iterator()).thenReturn(emptyIterator());
    when(cloudWatchLogsClientMock.describeLogGroupsPaginator(any(Consumer.class)))
        .thenReturn(expectedResponse);

    assertEquals(
        expectedResponse.iterator(),
        subject.getLambdaFunctionLogGroupsByNamePatternIterator("any"));
  }

  @Test
  void getLambdaFunctionLogGroupsByNamePatternIterator_ko() {
    reset(cloudWatchLogsClientMock);
    when(cloudWatchLogsClientMock.describeLogGroupsPaginator(any(Consumer.class)))
        .thenThrow(AwsServiceException.class);

    assertThrows(
        InternalServerErrorException.class,
        () -> subject.getLambdaFunctionLogGroupsByNamePatternIterator("failing"));
  }

  @Test
  void initiateLogInsightsQuery() {
    String expectedQueryId = "mock";
    StartQueryResponse expectedResponse =
        StartQueryResponse.builder().queryId(expectedQueryId).build();
    when(cloudWatchLogsClientMock.startQuery(any(Consumer.class))).thenReturn(expectedResponse);

    assertThrows(
        InternalServerErrorException.class,
        () -> subject.initiateLogInsightsQuery("query", Instant.now(), Instant.now(), list(51)));
    assertEquals(
        expectedQueryId,
        subject.initiateLogInsightsQuery("query", Instant.now(), Instant.now(), list(40)));
  }

  List<String> list(int nbElements) {
    return IntStream.range(0, nbElements).mapToObj(String::valueOf).toList();
  }

  @Test
  void getQueryResult() {
    GetQueryResultsResponse expectedResponse =
        GetQueryResultsResponse.builder().results(Collections.emptyList()).build();
    when(cloudWatchLogsClientMock.getQueryResults(any(Consumer.class)))
        .thenReturn(expectedResponse);

    assertEquals(expectedResponse, subject.getQueryResult("any"));
  }

  @Test
  void getLogGroupAllEventsUri() {
    assertEquals(
        URI.create(
            "https://dummy-region.console.aws.amazon.com/cloudwatch/home?region=dummy-region#logsV2:log-groups/log-group/log/log-events"),
        subject.getLogGroupAllEventsUri("log"));
  }

  @Test
  void computeLogUri() {
    assertEquals(
        URI.create(
            "https://dummy-region.console.aws.amazon.com/cloudwatch/home?region=dummy-region#logsV2:log-groups/log-group/log/log-events/logStream?start=2025-04-17T00%3A00%3A00Z"),
        subject.computeLogUri("log", "logStream", Instant.parse("2025-04-17T00:00:00.000Z")));
  }
}
