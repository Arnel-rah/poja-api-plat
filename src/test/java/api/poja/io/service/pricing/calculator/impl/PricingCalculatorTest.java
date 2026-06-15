package api.poja.io.service.pricing.calculator.impl;

import static api.poja.io.service.pricing.PricingMethod.TEN_MICRO;
import static api.poja.io.service.pricing.PricingMethod.TWENTY_MICRO;
import static org.junit.jupiter.api.Assertions.assertEquals;

import api.poja.io.conf.MockedThirdParties;
import api.poja.io.service.pricing.calculator.PricingCalculator;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class PricingCalculatorTest extends MockedThirdParties {
  @Autowired PricingCalculator subject;

  @Test
  void computeTenMicroPrice() {
    var computedPrice = subject.computePrice(TEN_MICRO, BigDecimal.valueOf(339968));
    assertEquals(new BigDecimal("3.39968"), computedPrice);
  }

  @Test
  void computeTwentyMicroPrice() {
    var computedPrice = subject.computePrice(TWENTY_MICRO, BigDecimal.valueOf(339968));
    assertEquals(new BigDecimal("6.79936"), computedPrice);
  }
}
