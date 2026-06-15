package api.poja.io.model.money;

import static api.poja.io.model.money.Currency.Family.EUR;
import static api.poja.io.model.money.Currency.Family.USD;
import static java.math.BigDecimal.ONE;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

public class ExchangeRateTest {
  public static final ExchangeRate FX_RATE =
      new ExchangeRate(new BigDecimal("1.1535"), new BigDecimal("0.8672"));

  final ExchangeRate subject = FX_RATE;

  @Test
  void should_return_usdEur_rate() {
    BigDecimal rate = subject.get(EUR, USD);

    assertEquals(new BigDecimal("1.1535"), rate);
  }

  @Test
  void should_return_eurUsd_rate() {
    BigDecimal rate = subject.get(USD, EUR);

    assertEquals(new BigDecimal("0.8672"), rate);
  }

  @Test
  void should_return_one_for_same_currency() {
    BigDecimal rate = subject.get(EUR, EUR);

    assertEquals(ONE, rate);
  }

  @Test
  void should_convert_money_to_same_currency() {
    var from = new BigDecimal("100");

    var actual = subject.apply(from, EUR, EUR);

    assertEquals(from, actual);
  }

  @Test
  void should_convert_money_to_different_currency1() {
    var from = new BigDecimal("100");

    var actual = subject.apply(from, EUR, USD);

    var expected = new BigDecimal("115.3500");
    assertEquals(expected, actual);
  }

  @Test
  void should_convert_money_to_different_currency2() {
    var from = new BigDecimal("100");

    var actual = subject.apply(from, USD, EUR);

    var expected = new BigDecimal("86.7200");
    assertEquals(expected, actual);
  }
}
