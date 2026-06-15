package api.poja.io.service.event;

import static api.poja.io.endpoint.rest.model.LogQueryStatus.CANCELLED;
import static api.poja.io.endpoint.rest.model.LogQueryStatus.COMPLETED;
import static api.poja.io.endpoint.rest.model.LogQueryStatus.FAILED;
import static api.poja.io.endpoint.rest.model.LogQueryStatus.PENDING;
import static api.poja.io.endpoint.rest.model.LogQueryStatus.RUNNING;
import static api.poja.io.endpoint.rest.model.LogQueryStatus.TIMED_OUT;
import static api.poja.io.endpoint.rest.model.LogQueryStatus.UNKNOWN;

import api.poja.io.aws.cloudwatch.CloudwatchComponent;
import api.poja.io.endpoint.event.consumer.model.exception.EventConsumptionBackOffException;
import api.poja.io.endpoint.event.model.GetLogQueryResultRequested;
import api.poja.io.endpoint.rest.model.LogQueryStatus;
import api.poja.io.service.logQuery.LogQueryService;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.cloudwatchlogs.model.GetQueryResultsResponse;
import software.amazon.awssdk.services.cloudwatchlogs.model.QueryStatus;

@Service
@AllArgsConstructor
public class GetLogQueryResultRequestedService implements Consumer<GetLogQueryResultRequested> {
  private final LogQueryService logQueryService;
  private final CloudwatchComponent cloudwatchComponent;

  @Override
  public void accept(GetLogQueryResultRequested getLogQueryResultRequested) {
    String queryId = getLogQueryResultRequested.getQueryId();
    GetQueryResultsResponse queryResult = cloudwatchComponent.getQueryResult(queryId);
    QueryStatus queryStatus = queryResult.status();
    if (QueryStatus.RUNNING.equals(queryStatus)) {
      throw new EventConsumptionBackOffException(
          "backoff because query is still running. domain QueryId = "
              + getLogQueryResultRequested.getDomainQueryId());
    }
    logQueryService.updateQueryStatus(
        getLogQueryResultRequested.getDomainQueryId(), toDomain(queryStatus));
  }

  private static LogQueryStatus toDomain(QueryStatus status) {
    return switch (status) {
      case SCHEDULED -> PENDING;
      case QueryStatus.RUNNING -> RUNNING;
      case COMPLETE -> COMPLETED;
      case FAILED -> FAILED;
      case CANCELLED -> CANCELLED;
      case TIMEOUT -> TIMED_OUT;
      case UNKNOWN, UNKNOWN_TO_SDK_VERSION -> UNKNOWN;
    };
  }
}
