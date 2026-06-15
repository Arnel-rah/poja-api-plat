package api.poja.io.model.billing;

import api.poja.io.service.pricing.PricingMethod;
import java.math.BigDecimal;
import java.time.Instant;

public interface AggregatedBillingInfoWithAwsByUserDTOProjection {
  BigDecimal getAmount();

  Instant getMaxComputeDatetime();

  Double getComputedDurationInMinutes();

  String getUserId();

  PricingMethod getPricingMethod();

  BigDecimal getAwsCost();

  Instant getAwsCostUpdateDatetime();

  // amount - awsCost; received from Sql since we are doing sorting on it anyway
  BigDecimal getCostMargin();
}
