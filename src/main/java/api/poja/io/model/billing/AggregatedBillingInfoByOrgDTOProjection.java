package api.poja.io.model.billing;

import api.poja.io.service.pricing.PricingMethod;
import java.math.BigDecimal;
import java.time.Instant;

public interface AggregatedBillingInfoByOrgDTOProjection {
  BigDecimal getAmount();

  Instant getMaxComputeDatetime();

  Double getComputedDurationInMinutes();

  String getUserId();

  String getOrgId();

  PricingMethod getPricingMethod();
}
