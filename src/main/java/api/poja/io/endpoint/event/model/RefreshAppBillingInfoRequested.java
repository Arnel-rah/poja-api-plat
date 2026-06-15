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
public class RefreshAppBillingInfoRequested extends PojaEvent {
  private final UUID id = randomUUID();
  private final String userId;
  private final String appId;
  private final RefreshOrgBillingInfoRequested refreshOrgBillingInfoRequested;

  public final PricingMethod getPricingMethod() {
    return refreshOrgBillingInfoRequested.getPricingMethod();
  }

  public final Instant getPricingCalculationRequestStartTime() {
    return refreshOrgBillingInfoRequested.getPricingCalculationRequestStartTime();
  }

  public final Instant getPricingCalculationRequestEndTime() {
    return refreshOrgBillingInfoRequested.getPricingCalculationRequestEndTime();
  }

  public final String getOrgId() {
    return refreshOrgBillingInfoRequested.getOrgId();
  }

  public final LocalDate getLocalDate() {
    return refreshOrgBillingInfoRequested.getLocalDate();
  }

  @Override
  public Duration maxConsumerDuration() {
    return Duration.ofSeconds(15);
  }

  @Override
  public Duration maxConsumerBackoffBetweenRetries() {
    return Duration.ofSeconds(90);
  }
}
