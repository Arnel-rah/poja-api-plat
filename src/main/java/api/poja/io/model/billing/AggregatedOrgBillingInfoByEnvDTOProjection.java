package api.poja.io.model.billing;

import java.math.BigDecimal;
import java.time.Instant;

public interface AggregatedOrgBillingInfoByEnvDTOProjection {
  BigDecimal getAmount();

  Instant getMaxComputeDatetime();

  Double getComputedDurationInMinutes();

  String getOrgId();

  String getAppId();

  String getEnvId();
}
