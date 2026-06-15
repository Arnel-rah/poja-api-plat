package api.poja.io.repository.model;

import static api.poja.io.repository.model.enums.InvoiceStatus.PAID;
import static jakarta.persistence.GenerationType.IDENTITY;
import static java.time.Instant.now;
import static java.time.temporal.ChronoUnit.MILLIS;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import java.time.Instant;
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
public class UserSubscription {
  @Id
  @GeneratedValue(strategy = IDENTITY)
  private String id;

  private String userId;
  @ManyToOne private Offer offer;
  private Instant subscriptionBeginDatetime;
  private Instant subscriptionEndDatetime;
  private boolean willRenew;

  @OneToOne private Invoice invoice;

  public Instant getSubscriptionBeginDatetime() {
    return subscriptionBeginDatetime.truncatedTo(MILLIS);
  }

  public Instant getSubscriptionEndDatetime() {
    return subscriptionEndDatetime.truncatedTo(MILLIS);
  }

  public boolean isActive() {
    var now = now();
    return PAID.equals(invoice.getStatus())
        && subscriptionBeginDatetime.isBefore(now)
        && (subscriptionEndDatetime == null || now.isBefore(subscriptionEndDatetime));
  }
}
