package api.poja.io.endpoint.event.model;

import static java.util.UUID.randomUUID;

import api.poja.io.service.pricing.PricingMethod;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper = false)
@ToString(callSuper = true)
@AllArgsConstructor
public class RefreshOrgBillingInfoRequested extends PojaEvent {
  private final UUID id = randomUUID();
  private final String userId;
  private final String orgId;
  private final RefreshUserBillingInfoRequested refreshUserBillingInfoTriggered;

  public final Instant getPricingCalculationRequestStartTime() {
    return refreshUserBillingInfoTriggered.getPricingCalculationRequestStartTime();
  }

  public final Instant getPricingCalculationRequestEndTime() {
    return refreshUserBillingInfoTriggered.getPricingCalculationRequestEndTime();
  }

  public final LocalDate getLocalDate() {
    return refreshUserBillingInfoTriggered.getLocalDate();
  }

  public final PricingMethod getPricingMethod() {
    return refreshUserBillingInfoTriggered.getPricingMethod();
  }

  @Override
  public Duration maxConsumerDuration() {
    return Duration.ofSeconds(45);
  }

  @Override
  public Duration maxConsumerBackoffBetweenRetries() {
    return Duration.ofSeconds(90);
  }
}
