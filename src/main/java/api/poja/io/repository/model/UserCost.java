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
import org.hibernate.annotations.JdbcTypeCode;

@Entity
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Table(name = "\"user_cost\"")
@EqualsAndHashCode
@ToString
public class UserCost implements Serializable {
  @Id private String id;

  private BigDecimal amount;

  @JdbcTypeCode(NAMED_ENUM)
  @Enumerated(STRING)
  private PaymentRequestPeriod month;

  private int year;

  private Instant updatedAt;

  @Column(updatable = false)
  private String userId;
}
