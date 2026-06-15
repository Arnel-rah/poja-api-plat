package api.poja.io.repository.model;

import static api.poja.io.model.money.Currency.EUR;

import api.poja.io.model.money.Money;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
@ToString
public class Offer {
  @Id private String id;
  private String name;
  private long maxApps;
  private BigDecimal price;

  public long getMaxUserGroups() {
    return Math.ceilDiv(maxApps, 2) + 1;
  }

  public Money getMonthlyPriceAsMoney() {
    return new Money(price, EUR);
  }
}
