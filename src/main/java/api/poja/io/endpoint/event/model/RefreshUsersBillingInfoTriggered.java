package api.poja.io.endpoint.event.model;

import static java.time.Instant.now;
import static java.time.ZoneOffset.UTC;
import static java.util.UUID.randomUUID;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper = false)
@ToString(callSuper = true)
public class RefreshUsersBillingInfoTriggered extends PojaEvent {
  private final UUID id;
  private final LocalDate utcLocalDate;
  private final Instant pricingCalculationRequestStartTime;
  private final Instant pricingCalculationRequestEndTime;

  @Builder
  public RefreshUsersBillingInfoTriggered(
      UUID id,
      LocalDate utcLocalDate,
      Instant pricingCalculationRequestStartTime,
      Instant pricingCalculationRequestEndTime) {
    this.id = id;
    this.utcLocalDate = utcLocalDate;
    this.pricingCalculationRequestStartTime = pricingCalculationRequestStartTime;
    this.pricingCalculationRequestEndTime = pricingCalculationRequestEndTime;
  }

  public RefreshUsersBillingInfoTriggered() {
    this.id = randomUUID();
    this.utcLocalDate = LocalDate.now(UTC);
    this.pricingCalculationRequestStartTime = utcLocalDate.atStartOfDay(UTC).toInstant();
    this.pricingCalculationRequestEndTime = now();
  }

  @Override
  public Duration maxConsumerDuration() {
    return Duration.ofSeconds(40);
  }

  @Override
  public Duration maxConsumerBackoffBetweenRetries() {
    return Duration.ofSeconds(90);
  }
}
