package api.poja.io.model.money;

import static java.math.BigDecimal.ONE;
import static java.math.RoundingMode.HALF_EVEN;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
@Getter
public enum Currency {
  USD(BigDecimal.valueOf(1_000_000), Family.USD),
  CENTS_USD(BigDecimal.valueOf(10_000), Family.USD),
  MICRO_USD(ONE, Family.USD),

  EUR(BigDecimal.valueOf(1_000_000), Family.EUR),
  CENTS_EUR(BigDecimal.valueOf(10_000), Family.EUR),
  MICRO_EUR(ONE, Family.EUR);

  private final BigDecimal valueInMicroUnit;
  private final Family family;

  /*package-private*/ Currency(BigDecimal valueInMicroUnits, Family family) {
    this.valueInMicroUnit = valueInMicroUnits;
    this.family = family;
  }

  public BigDecimal toMicroUnits(BigDecimal amount) {
    return amount.multiply(valueInMicroUnit);
  }

  public BigDecimal fromMicroUnits(BigDecimal microAmount) {
    return microAmount.divide(valueInMicroUnit, HALF_EVEN);
  }

  public enum Family {
    USD,
    EUR
  }
}
