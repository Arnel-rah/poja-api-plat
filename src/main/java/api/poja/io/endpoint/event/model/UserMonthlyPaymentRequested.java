package api.poja.io.endpoint.event.model;

import static api.poja.io.model.money.Money.ZERO;

import api.poja.io.model.money.Money;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper = false)
@ToString(callSuper = true)
public class UserMonthlyPaymentRequested extends PojaEvent {
  private String userId;
  private String customerId;
  private String paymentRequestId;
  private YearMonth yearMonth;
  private final List<ItemToPay> itemsToPay;

  @Builder
  public UserMonthlyPaymentRequested(
      String userId, String customerId, String paymentRequestId, YearMonth yearMonth) {
    this.userId = userId;
    this.customerId = customerId;
    this.yearMonth = yearMonth;
    this.paymentRequestId = paymentRequestId;
    this.itemsToPay = new ArrayList<>();
  }

  public void addInvoiceItem(String id, String description, Money amount) {
    ItemToPay e = new ItemToPay(id, customerId, description, amount);
    itemsToPay.add(e);
  }

  public void addInvoiceItem(String id, String description, Money unitPrice, Long quantity) {
    ItemToPay e = new ItemToPay(id, customerId, description, unitPrice, quantity);
    itemsToPay.add(e);
  }

  public Money computeDueAmount() {
    return itemsToPay.stream().map(ItemToPay::amount).reduce(ZERO, Money::add);
  }

  @Override
  public Duration maxConsumerDuration() {
    return Duration.ofSeconds(30);
  }

  @Override
  public Duration maxConsumerBackoffBetweenRetries() {
    return Duration.ofSeconds(30);
  }

  public record ItemToPay(
      String id,
      String customerId,
      String description,
      Money amount,
      Money unitPrice,
      Long quantity) {
    public ItemToPay(String id, String customerId, String description, Money amount) {
      this(id, customerId, description, amount, null, null);
    }

    public ItemToPay(
        String id, String customerId, String description, Money unitPrice, Long quantity) {
      this(
          id,
          customerId,
          description,
          new Money(
              unitPrice.amount().multiply(BigDecimal.valueOf(quantity)), unitPrice.currency()),
          unitPrice,
          quantity);
    }
  }
}
