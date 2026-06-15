package api.poja.io.service.pricing.calculator.impl;

import api.poja.io.service.pricing.PricingMethod;
import api.poja.io.service.pricing.calculator.PricingCalculator;
import java.math.BigDecimal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Primary
@Component
@Slf4j
class PricingCalculatorFacade implements PricingCalculator {
  private final AbstractPricingCalculator tenMicroPricingCalculator;
  private final AbstractPricingCalculator twentyMicroPricingCalculator;

  public PricingCalculatorFacade(
      @Qualifier("tenMicroPricingCalculator") AbstractPricingCalculator tenMicroPricingCalculator,
      @Qualifier("twentyMicroPricingCalculator")
          AbstractPricingCalculator twentyMicroPricingCalculator) {
    this.tenMicroPricingCalculator = tenMicroPricingCalculator;
    this.twentyMicroPricingCalculator = twentyMicroPricingCalculator;
  }

  private AbstractPricingCalculator getPricingCalculator(PricingMethod method) {
    if (tenMicroPricingCalculator.supports(method)) {
      return tenMicroPricingCalculator;
    }
    if (twentyMicroPricingCalculator.supports(method)) {
      return twentyMicroPricingCalculator;
    }
    throw new IllegalArgumentException("unsupported pricing method: " + method);
  }

  @Override
  public boolean supports(PricingMethod pricingMethod) {
    throw new UnsupportedOperationException(
        "operation was not meant to be supported by this class");
  }

  @Override
  public BigDecimal computePrice(PricingMethod method, BigDecimal totalMemoryDurationMinutes) {
    return getPricingCalculator(method).computePrice(totalMemoryDurationMinutes);
  }
}
