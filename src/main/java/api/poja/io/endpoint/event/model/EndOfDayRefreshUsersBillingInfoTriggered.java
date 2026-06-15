package api.poja.io.endpoint.event.model;

import static java.time.ZoneOffset.UTC;
import static java.util.UUID.randomUUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Duration;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@EqualsAndHashCode(callSuper = false)
@ToString(callSuper = true)
@AllArgsConstructor
@Data
public class EndOfDayRefreshUsersBillingInfoTriggered extends PojaEvent {
  private final RefreshUsersBillingInfoTriggered refreshUsersBillingInfoTriggered;

  @JsonProperty("local_date")
  private final LocalDate localDate;

  /**
   * Class will always be initialized past 00:00 of next day, this class is needed to compute
   * yesterday's billing info with maximum precision
   */
  public EndOfDayRefreshUsersBillingInfoTriggered() {
    this.localDate = LocalDate.now(UTC).minusDays(1);
    this.refreshUsersBillingInfoTriggered =
        RefreshUsersBillingInfoTriggered.builder()
            .id(randomUUID())
            .utcLocalDate(this.localDate)
            .pricingCalculationRequestStartTime(this.localDate.atStartOfDay(UTC).toInstant())
            .pricingCalculationRequestEndTime(this.localDate.atTime(23, 59, 59).toInstant(UTC))
            .build();
  }

  @JsonCreator
  public EndOfDayRefreshUsersBillingInfoTriggered(LocalDate localDate) {
    this.localDate = localDate == null ? LocalDate.now(UTC).minusDays(1) : localDate;
    this.refreshUsersBillingInfoTriggered =
        RefreshUsersBillingInfoTriggered.builder()
            .id(randomUUID())
            .utcLocalDate(this.localDate)
            .pricingCalculationRequestStartTime(this.localDate.atStartOfDay(UTC).toInstant())
            .pricingCalculationRequestEndTime(this.localDate.atTime(23, 59, 59).toInstant(UTC))
            .build();
  }

  public RefreshUsersBillingInfoTriggered asRefreshUsersBillingInfoTriggered() {
    return refreshUsersBillingInfoTriggered;
  }

  @Override
  public Duration maxConsumerDuration() {
    return Duration.ofSeconds(40);
  }

  @Override
  public Duration maxConsumerBackoffBetweenRetries() {
    return Duration.ofSeconds(30);
  }
}
