package api.poja.io.service.pricing;

import static lombok.AccessLevel.PACKAGE;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = PACKAGE)
public enum PricingMethod {
  TEN_MICRO("10µ", new BigDecimal("0.00001")),
  TWENTY_MICRO("20µ", new BigDecimal("0.00002"));
  private final String name;
  private final BigDecimal value;
}
