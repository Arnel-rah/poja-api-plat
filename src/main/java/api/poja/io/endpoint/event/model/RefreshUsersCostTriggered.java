package api.poja.io.endpoint.event.model;

import static java.time.ZoneOffset.UTC;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Duration;
import java.time.LocalDate;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@EqualsAndHashCode(callSuper = false)
@ToString(callSuper = true)
@Data
public class RefreshUsersCostTriggered extends PojaEvent {
  @JsonProperty("date")
  private final LocalDate date;

  @JsonCreator
  public RefreshUsersCostTriggered(LocalDate date) {
    this.date = date == null ? LocalDate.now(UTC) : date;
  }

  @Override
  public Duration maxConsumerDuration() {
    return Duration.ofSeconds(30);
  }

  @Override
  public Duration maxConsumerBackoffBetweenRetries() {
    return Duration.ofSeconds(30);
  }
}
