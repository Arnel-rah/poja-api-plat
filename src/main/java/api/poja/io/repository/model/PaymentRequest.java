package api.poja.io.repository.model;

import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.GenerationType.IDENTITY;
import static org.hibernate.type.SqlTypes.NAMED_ENUM;

import api.poja.io.repository.model.enums.PaymentRequestPeriod;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import java.time.Instant;
import java.time.YearMonth;
import java.util.List;
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
public class PaymentRequest {
  @Id
  @GeneratedValue(strategy = IDENTITY)
  private String id;

  private Instant requestInstant;
  private int year;

  @JdbcTypeCode(NAMED_ENUM)
  @Enumerated(STRING)
  private PaymentRequestPeriod period;

  @OneToMany(mappedBy = "paymentRequest")
  private List<UserPaymentRequest> userPaymentRequest;

  @JsonIgnore
  public YearMonth getYearMonth() {
    return YearMonth.of(year, period.getCorrespondingJavaMonth());
  }
}
