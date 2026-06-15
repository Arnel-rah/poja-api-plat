package api.poja.io.repository.model;

import static jakarta.persistence.EnumType.STRING;
import static org.hibernate.type.SqlTypes.NAMED_ENUM;

import api.poja.io.repository.model.enums.PaymentRequestPeriod;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serializable;
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
@Table(name = "\"user_billing_discount\"")
@EqualsAndHashCode
@ToString
public class UserBillingDiscount implements Serializable {
  @Id private String id;

  private BigDecimal amount;
  private String description;
  private int year;

  @JdbcTypeCode(NAMED_ENUM)
  @Enumerated(STRING)
  private PaymentRequestPeriod month;

  @CreationTimestamp
  @Column(updatable = false)
  private Instant creationDatetime;

  private String userId;
}
