package api.poja.io.model.billing;

import java.math.BigDecimal;
import java.time.Instant;

public interface AggregatedBillingInfoByEnvDTOProjection {
  BigDecimal getAmount();

  BigDecimal getMemoryDuration();

  Instant getMaxComputeDatetime();

  Double getComputedDurationInMinutes();

  String getUserId();

  String getEnvId();

  String getAppId();
}
