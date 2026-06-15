package api.poja.io.model.money;

import static api.poja.io.model.money.Currency.CENTS_EUR;
import static api.poja.io.model.money.Currency.MICRO_EUR;
import static api.poja.io.model.money.Money.ZERO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class MoneyTest {
  @Test
  void testEquals() {
    Money dollar1 = new Money(BigDecimal.valueOf(1), Currency.EUR);
    Money dollar2 = new Money(BigDecimal.valueOf(1), Currency.EUR);
    Money microDollar = new Money(BigDecimal.valueOf(1_000_000), MICRO_EUR);

    assertEquals(dollar1, dollar2);
    assertEquals(dollar1, microDollar);
    Money dollar3 = new Money(BigDecimal.valueOf(2), Currency.EUR);
    assertNotEquals(dollar1, dollar3);
  }

  @Test
  void testCompareTo() {
    Money dollar1 = new Money(BigDecimal.valueOf(1), Currency.EUR);
    Money dollar2 = new Money(BigDecimal.valueOf(1), Currency.EUR);
    Money microDollar = new Money(BigDecimal.valueOf(1_000_000), MICRO_EUR);
    Money cent = new Money(BigDecimal.valueOf(100), CENTS_EUR);

    assertEquals(0, dollar1.compareTo(dollar2));
    assertEquals(0, dollar1.compareTo(microDollar));
    assertTrue(dollar1.compareTo(new Money(BigDecimal.valueOf(2), Currency.EUR)) < 0);
    assertEquals(0, dollar1.compareTo(cent)); // 1 Dollar = 100 Cents, so the values should be equal
  }

  @Test
  void testConvertCurrency() {
    Money dollar = new Money(BigDecimal.valueOf(1), Currency.EUR);
    Money microDollar = dollar.convertCurrency(MICRO_EUR);
    Money cent = dollar.convertCurrency(CENTS_EUR);

    assertEquals(BigDecimal.valueOf(1_000_000), microDollar.amount());
    assertEquals(BigDecimal.valueOf(100), cent.amount());
  }

  @Test
  void testAdd() {
    Money dollar1 = new Money(BigDecimal.valueOf(1), Currency.EUR);
    Money dollar2 = new Money(BigDecimal.valueOf(2), Currency.EUR);

    // Test adding two Money objects in the same currency
    Money result = dollar1.add(dollar2);
    assertEquals(BigDecimal.valueOf(3), result.amount());
    assertEquals(Currency.EUR, result.currency());

    // Test adding Money objects in different currencies
    Money microDollar = new Money(BigDecimal.valueOf(1_000_000), MICRO_EUR);
    Money sum = dollar1.add(microDollar);
    assertEquals(BigDecimal.valueOf(2), sum.amount());
    assertEquals(Currency.EUR, sum.currency());
  }

  @Test
  void testZeroConstant() {
    Money zero = ZERO;
    assertEquals(BigDecimal.ZERO, zero.amount());
    assertEquals(MICRO_EUR, zero.currency());
  }

  @Test
  void crossConversion_shouldBe_illegal_without_exchangeRate() {
    var dollar = new Money(BigDecimal.valueOf(1), Currency.USD);
    var euro = new Money(BigDecimal.valueOf(1), Currency.EUR);

    assertThrows(
        IllegalArgumentException.class,
        () -> dollar.convertCurrency(Currency.EUR),
        "Cross-family conversion from USD to EUR requires an exchange rate provider.");

    assertThrows(
        IllegalArgumentException.class,
        () -> euro.add(dollar),
        "Cross-family conversion from EUR to USD requires an exchange rate provider.");
  }

  @Test
  void crossConversion_shouldBe_legal_with_exchangeRate() {
    // var dollar = new Money(BigDecimal.valueOf(2.3), Currency.USD);

    // ExchangeRateProvider provider = (from, to) -> BigDecimal.valueOf(0.87);
    // var convertedDollar = dollar.convertCurrency(Currency.EUR, provider);
    // assertEquals(BigDecimal.valueOf(2.001), convertedDollar.amount());
    // assertEquals(Currency.EUR, convertedDollar.currency());
  }
}
