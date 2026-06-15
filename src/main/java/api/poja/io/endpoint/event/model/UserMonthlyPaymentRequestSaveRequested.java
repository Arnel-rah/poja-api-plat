package api.poja.io.endpoint.event.model;

import api.poja.io.repository.model.enums.PaymentRequestPeriod;
import api.poja.io.service.pricing.PricingMethod;
import java.time.Duration;
import java.time.Month;
import java.time.Year;
import java.time.YearMonth;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper = false)
@ToString(callSuper = true)
@Builder
public class UserMonthlyPaymentRequestSaveRequested extends PojaEvent {
  private String userId;
  private String paymentRequestId;
  private String customerId;
  private PaymentRequestPeriod period;
  private Year year;
  private PricingMethod pricingMethod;

  public YearMonth getYearMonth() {
    return YearMonth.of(year.getValue(), Month.valueOf(period.name()));
  }

  @Override
  public Duration maxConsumerDuration() {
    return Duration.ofSeconds(70);
  }

  @Override
  public Duration maxConsumerBackoffBetweenRetries() {
    return Duration.ofSeconds(60);
  }
}
