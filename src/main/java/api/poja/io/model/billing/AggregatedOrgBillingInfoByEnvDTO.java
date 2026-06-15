package api.poja.io.model.billing;

import java.math.BigDecimal;
import java.time.Instant;

public record AggregatedOrgBillingInfoByEnvDTO(
    BigDecimal amount,
    Instant maxComputedatetime,
    Double computedDurationInMinutes,
    String orgId,
    String appId,
    String envId)
    implements AggregatedOrgBillingInfoByEnvDTOProjection {
  public AggregatedOrgBillingInfoByEnvDTO(
      BigDecimal amount,
      Instant maxComputedatetime,
      Double computedDurationInMinutes,
      String orgId,
      String appId,
      String envId) {
    this.amount = amount == null ? BigDecimal.ZERO : amount;
    this.maxComputedatetime = maxComputedatetime;
    this.computedDurationInMinutes = computedDurationInMinutes;
    this.orgId = orgId;
    this.envId = envId;
    this.appId = appId;
  }

  @Override
  public BigDecimal getAmount() {
    return amount;
  }

  @Override
  public Instant getMaxComputeDatetime() {
    return maxComputedatetime;
  }

  @Override
  public Double getComputedDurationInMinutes() {
    return computedDurationInMinutes;
  }

  @Override
  public String getOrgId() {
    return orgId;
  }

  @Override
  public String getAppId() {
    return appId;
  }

  @Override
  public String getEnvId() {
    return envId;
  }
}
