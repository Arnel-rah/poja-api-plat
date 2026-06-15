package api.poja.io.endpoint.event.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Duration;
import java.time.YearMonth;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper = false)
@ToString(callSuper = true)
public class UserEnvBillingInfoUpdateFromQueryRequested extends PojaEvent {
  @JsonCreator
  public UserEnvBillingInfoUpdateFromQueryRequested(
      String queryId, String userId, String envId, RefreshEnvBillingInfoRequested parentEvent) {
    this.queryId = queryId;
    this.userId = userId;
    this.envId = envId;
    this.parentEvent = parentEvent;
  }

  public YearMonth getYearMonth() {
    return YearMonth.from(parentEvent.getLocalDate());
  }

  @JsonProperty("query_id")
  private final String queryId;

  @JsonProperty("user_id")
  private final String userId;

  @JsonProperty("env_id")
  private final String envId;

  @JsonProperty("parent_event")
  private final RefreshEnvBillingInfoRequested parentEvent;

  @Override
  public Duration maxConsumerDuration() {
    return Duration.ofSeconds(10);
  }

  @Override
  public Duration maxConsumerBackoffBetweenRetries() {
    return Duration.ofSeconds(90);
  }
}
