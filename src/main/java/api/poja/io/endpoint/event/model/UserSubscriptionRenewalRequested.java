package api.poja.io.endpoint.event.model;

import static java.time.ZoneOffset.UTC;
import static java.time.temporal.ChronoUnit.MILLIS;

import api.poja.io.repository.model.UserSubscription;
import java.time.Duration;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@EqualsAndHashCode(callSuper = false)
@ToString(callSuper = true)
@AllArgsConstructor
@Data
@Builder
public class UserSubscriptionRenewalRequested extends PojaEvent {
  private UsersSubscriptionRenewalRequested parentEvent;
  private UserSubscription userCurrentSubscription;

  public Instant getNextSubscriptionBegin() {
    return parentEvent.getNextMonth().atDay(1).atStartOfDay(UTC).toInstant().truncatedTo(MILLIS);
  }

  @Override
  public Duration maxConsumerDuration() {
    return Duration.ofSeconds(30);
  }

  @Override
  public Duration maxConsumerBackoffBetweenRetries() {
    return Duration.ofMinutes(1);
  }
}
