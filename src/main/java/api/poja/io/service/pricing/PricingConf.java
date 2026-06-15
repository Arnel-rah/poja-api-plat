package api.poja.io.service.pricing;

import static api.poja.io.model.money.Currency.EUR;

import api.poja.io.model.money.Money;
import java.math.BigDecimal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public record PricingConf(@Value("${pricing.free.tier}") BigDecimal freeTier) {
  public Money freeTierAsMoney() {
    return new Money(freeTier, EUR);
  }
}
