package api.poja.io.model.money;

import static api.poja.io.model.money.Currency.MICRO_EUR;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

public record Money(
    @JsonProperty("amount") BigDecimal amount, @JsonProperty("currenty") Currency currency)
    implements Comparable<Money> {
  public static final Money ZERO = new Money(BigDecimal.ZERO, MICRO_EUR);

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Money other = (Money) o;

    // Convert both to microunits for a fair comparison
    BigDecimal thisMicro = this.currency.toMicroUnits(this.amount);
    BigDecimal otherMicro = other.currency.toMicroUnits(other.amount);

    return thisMicro.compareTo(otherMicro) == 0;
  }

  @Override
  public int hashCode() {
    // Normalize to microunits for consistent hash code
    BigDecimal microValue = currency.toMicroUnits(amount);
    return 31 * microValue.hashCode();
  }

  @Override
  public int compareTo(Money o) {
    BigDecimal thisMicro = this.currency.toMicroUnits(this.amount);
    BigDecimal otherMicro = o.currency.toMicroUnits(o.amount);
    return thisMicro.compareTo(otherMicro);
  }

  public Money convertCurrency(Currency destination) {
    if (destination == currency) {
      return this;
    }
    if (destination.family() != currency.family()) {
      throw new IllegalArgumentException(
          "Cross-family conversion from "
              + currency
              + " to "
              + destination
              + " requires an exchange rate provider.");
    }
    BigDecimal micro = currency.toMicroUnits(amount);
    return new Money(destination.fromMicroUnits(micro), destination);
  }

  public Money convertCurrency(Currency dest, ExchangeRate exchangeRate) {
    if (dest.family() == currency.family()) {
      return convertCurrency(dest);
    }
    BigDecimal microSource = currency.toMicroUnits(amount);
    BigDecimal microDest = exchangeRate.apply(microSource, currency.family(), dest.family());
    return new Money(dest.fromMicroUnits(microDest), dest);
  }

  public Money add(Money money) {
    return new Money(amount.add(money.convertCurrency(currency).amount), currency);
  }
}
