package api.poja.io.repository.model;

import static api.poja.io.repository.model.enums.InvoiceStatus.PAID;
import static api.poja.io.repository.model.enums.InvoiceStatus.PROCESSING;
import static api.poja.io.repository.model.enums.InvoiceStatus.REQUIRES_PAYMENT_METHOD;
import static api.poja.io.repository.model.enums.InvoiceStatus.UNKNOWN;
import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.GenerationType.IDENTITY;
import static java.time.YearMonth.now;
import static java.time.ZoneOffset.UTC;
import static org.hibernate.type.SqlTypes.NAMED_ENUM;

import api.poja.io.repository.model.enums.InvoiceStatus;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
public class UserPaymentRequest {
  @Id
  @GeneratedValue(strategy = IDENTITY)
  private String id;

  private long amount;
  private long discountAmount;

  private String invoiceId;
  private String invoiceUrl;

  @JdbcTypeCode(NAMED_ENUM)
  @Enumerated(STRING)
  private InvoiceStatus invoiceStatus;

  public boolean isDefinitelyUnpaid() {
    if (REQUIRES_PAYMENT_METHOD.equals(invoiceStatus)) {
      return true;
    }
    return (!UNKNOWN.equals(invoiceStatus))
        && (!PROCESSING.equals(invoiceStatus))
        && (!PAID.equals(invoiceStatus));
  }

  public boolean isOneMonthLateAndDefinitelyUnpaid() {
    boolean b = paymentRequest.getYearMonth().atEndOfMonth().isBefore(now(UTC).atDay(1));
    return isDefinitelyUnpaid() && b;
  }

  @ManyToOne
  @JoinColumn(name = "payment_request_id")
  private PaymentRequest paymentRequest;

  private String userId;
}
