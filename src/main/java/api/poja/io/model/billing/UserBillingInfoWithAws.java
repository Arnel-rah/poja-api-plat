package api.poja.io.model.billing;

import api.poja.io.service.pricing.PricingMethod;
import java.math.BigDecimal;
import java.time.Instant;
import javax.annotation.Nullable;
import lombok.Builder;

// TODO(quickfix): computedPrice and awsCost should never be null
@Builder
public record UserBillingInfoWithAws(
    String userId,
    PricingMethod pricingMethod,
    @Nullable BigDecimal computedPrice,
    Instant computeDatetime,
    double computedDurationInMinutes,
    Instant computationIntervalEnd,
    BigDecimal awsCost,
    BigDecimal costMargin,
    Instant awsCostUpdateDatetime) {}
