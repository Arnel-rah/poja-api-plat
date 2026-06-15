package api.poja.io.endpoint.event.model;

import java.time.Duration;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@AllArgsConstructor
@Data
@EqualsAndHashCode(callSuper = false)
@ToString(callSuper = true)
@Builder(toBuilder = true)
public class RefreshUserStatusRequested extends PojaEvent {
  private String userId;
  private final Instant pricingCalculationRequestStartTime;
  private final Instant pricingCalculationRequestEndTime;

  public final Instant getPricingCalculationRequestStartTime() {
    return pricingCalculationRequestStartTime;
  }

  public final Instant getPricingCalculationRequestEndTime() {
    return pricingCalculationRequestEndTime;
  }

  @Override
  public Duration maxConsumerDuration() {
    return Duration.ofSeconds(10);
  }

  @Override
  public Duration maxConsumerBackoffBetweenRetries() {
    return Duration.ofSeconds(30);
  }
}
