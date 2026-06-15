package api.poja.io.model.billing;

import api.poja.io.service.pricing.PricingMethod;
import java.math.BigDecimal;
import java.time.Instant;

public interface AggregatedBillingInfoByUserDTOProjection {
  BigDecimal getAmount();

  Instant getMaxComputeDatetime();

  Double getComputedDurationInMinutes();

  String getUserId();

  PricingMethod getPricingMethod();
}
