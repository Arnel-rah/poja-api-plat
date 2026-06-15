package api.poja.io.repository.model;

import static api.poja.io.model.money.Currency.EUR;
import static api.poja.io.repository.model.enums.InvoiceStatus.DRAFT;
import static api.poja.io.repository.model.enums.InvoiceStatus.OPEN;
import static api.poja.io.repository.model.enums.InvoiceStatus.PAID;
import static api.poja.io.repository.model.enums.InvoiceStatus.PROCESSING;
import static api.poja.io.repository.model.enums.InvoiceStatus.UNKNOWN;
import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.GenerationType.IDENTITY;
import static java.util.UUID.randomUUID;
import static org.hibernate.type.SqlTypes.NAMED_ENUM;

import api.poja.io.model.money.Money;
import api.poja.io.repository.model.enums.InvoiceStatus;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;

@Entity
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
@ToString
public class Invoice {
  @Id
  @GeneratedValue(strategy = IDENTITY)
  private String id;

  private BigDecimal amount;
  private String invoiceId;
  private String invoiceUrl;
  private String userId;

  @Enumerated(STRING)
  @JdbcTypeCode(NAMED_ENUM)
  private InvoiceStatus status;

  public Money getAmountAsMoney() {
    return new Money(amount, EUR);
  }

  public boolean canBePaid() {
    return status.canBePaid();
  }

  public boolean hasReachedFinalStatus() {
    return !UNKNOWN.equals(status)
        && !DRAFT.equals(status)
        && !OPEN.equals(status)
        && !PROCESSING.equals(status);
  }

  public static Invoice createPaidInvoice(String userId, BigDecimal amountInUsd) {
    return Invoice.builder()
        .id(randomUUID().toString())
        .invoiceId(randomUUID().toString())
        .invoiceUrl(null)
        .userId(userId)
        .amount(amountInUsd)
        .status(PAID)
        .build();
  }
}
