package api.poja.io.model.money;

import static api.poja.io.model.money.Currency.Family.EUR;
import static api.poja.io.model.money.Currency.Family.USD;
import static java.math.BigDecimal.ONE;

import api.poja.io.datastructure.BiParametricSupplier;
import api.poja.io.model.money.Currency.Family;
import java.math.BigDecimal;
import java.util.Map;
import org.apache.commons.lang3.function.TriFunction;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @deprecated use up-to-date exchange rate APIs instead, e.g. <a
 *     href="https://www.exchangerate-api.com/">exchangerate-api</a>
 */
@Component
@Deprecated(forRemoval = false)
public class ExchangeRate
    implements BiParametricSupplier<Currency.Family, Currency.Family, BigDecimal>,
        TriFunction<BigDecimal, Family, Family, BigDecimal> {
  private final Map<String, BigDecimal> rates;

  public ExchangeRate(
      @Value("${exchange.rate.eur.usd:1}") BigDecimal eurUsd,
      @Value("${exchange.rate.usd.eur:1}") BigDecimal usdEur) {
    this.rates =
        Map.of(
            keyOf(EUR, USD), eurUsd,
            keyOf(USD, EUR), usdEur);
  }

  @Override
  public BigDecimal get(Family a, Family b) {
    return rates.getOrDefault(keyOf(a, b), ONE);
  }

  private static String keyOf(Family a, Family b) {
    return String.format("%s.%s", a.name(), b.name());
  }

  @Override
  public BigDecimal apply(BigDecimal amount, Family source, Family dest) {
    if (source == dest) {
      return amount;
    }
    BigDecimal exchangeRate = get(source, dest);
    return amount.multiply(exchangeRate);
  }
}
