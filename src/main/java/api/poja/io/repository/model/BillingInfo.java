package api.poja.io.repository.model;

import static api.poja.io.model.money.Currency.EUR;
import static jakarta.persistence.EnumType.STRING;
import static org.hibernate.type.SqlTypes.NAMED_ENUM;

import api.poja.io.model.money.Money;
import api.poja.io.repository.model.enums.BillingInfoComputeStatus;
import api.poja.io.service.pricing.PricingMethod;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;

@Entity
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Table(name = "\"billing_info\"")
@EqualsAndHashCode
@ToString
public class BillingInfo {
  @Id private String id;

  @CreationTimestamp private Instant creationDatetime;

  private Instant computeDatetime;
  private Instant computationIntervalEnd;

  private String userId;
  private String appId;
  private String envId;
  private String queryId;
  private String orgId;

  @JdbcTypeCode(NAMED_ENUM)
  @Enumerated(STRING)
  private PricingMethod pricingMethod;

  private BigDecimal computedPrice;

  public Money getComputedPriceAsMoney() {
    return new Money(computedPrice, EUR);
  }

  private double computedDurationInMinutes;

  private BigDecimal computedMemoryDurationInMbMinutes;

  @JdbcTypeCode(NAMED_ENUM)
  @Enumerated(STRING)
  private BillingInfoComputeStatus status;
}
