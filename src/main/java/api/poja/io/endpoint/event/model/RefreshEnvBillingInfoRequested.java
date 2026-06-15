package api.poja.io.endpoint.event.model;

import static api.poja.io.endpoint.event.EventStack.EVENT_STACK_2;
import static java.util.UUID.randomUUID;

import api.poja.io.endpoint.event.EventStack;
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
public class RefreshEnvBillingInfoRequested extends PojaEvent {
  private final UUID id = randomUUID();
  private final String envId;
  private final String userId;
  private final String appId;
  private final RefreshAppBillingInfoRequested refreshAppBillingInfoRequested;

  public final PricingMethod getPricingMethod() {
    return refreshAppBillingInfoRequested.getPricingMethod();
  }

  public final Instant getPricingCalculationRequestStartTime() {
    return refreshAppBillingInfoRequested.getPricingCalculationRequestStartTime();
  }

  public final Instant getPricingCalculationRequestEndTime() {
    return refreshAppBillingInfoRequested.getPricingCalculationRequestEndTime();
  }

  public final String getOrgId() {
    return refreshAppBillingInfoRequested.getOrgId();
  }

  public final LocalDate getLocalDate() {
    return refreshAppBillingInfoRequested.getLocalDate();
  }

  @Override
  public Duration maxConsumerDuration() {
    return Duration.ofSeconds(10);
  }

  @Override
  public Duration maxConsumerBackoffBetweenRetries() {
    return Duration.ofSeconds(90);
  }

  @Override
  public EventStack getEventStack() {
    return EVENT_STACK_2;
  }
}
