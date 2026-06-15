package api.poja.io.model.billing;

import api.poja.io.service.pricing.PricingMethod;
import java.math.BigDecimal;
import java.time.Instant;

public record AggregatedBillingInfoByOrgDTO(
    BigDecimal amount,
    Instant maxComputeDatetime,
    Double computedDurationInMinutes,
    String userId,
    String orgId,
    PricingMethod pricingMethod)
    implements AggregatedBillingInfoByOrgDTOProjection {
  @Override
  public BigDecimal getAmount() {
    return amount;
  }

  @Override
  public Instant getMaxComputeDatetime() {
    return maxComputeDatetime;
  }

  @Override
  public Double getComputedDurationInMinutes() {
    return computedDurationInMinutes;
  }

  @Override
  public String getUserId() {
    return userId;
  }

  @Override
  public String getOrgId() {
    return orgId;
  }

  @Override
  public PricingMethod getPricingMethod() {
    return pricingMethod;
  }
}
