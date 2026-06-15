package api.poja.io.service.event;

import static api.poja.io.model.money.Currency.CENTS_EUR;
import static api.poja.io.model.money.Currency.EUR;
import static api.poja.io.model.money.Money.ZERO;
import static api.poja.io.repository.model.enums.InvoiceStatus.DRAFT;
import static api.poja.io.repository.model.enums.InvoiceStatus.REQUIRES_ACTION;
import static java.lang.Math.min;

import api.poja.io.endpoint.event.model.UserMonthlyPaymentRequested;
import api.poja.io.model.exception.ApiException;
import api.poja.io.model.money.Money;
import api.poja.io.repository.model.UserBillingDiscount;
import api.poja.io.repository.model.UserPaymentRequest;
import api.poja.io.repository.model.enums.InvoiceStatus;
import api.poja.io.repository.model.enums.PaymentRequestPeriod;
import api.poja.io.service.UserBillingDiscountService;
import api.poja.io.service.UserPaymentRequestService;
import api.poja.io.service.pricing.PricingConf;
import api.poja.io.service.stripe.StripeService;
import api.poja.io.sys.platform.SaasOnly;
import com.stripe.model.Invoice;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
@SaasOnly
public class UserMonthlyPaymentRequestedService implements Consumer<UserMonthlyPaymentRequested> {
  private final PricingConf pricingConf;
  private final StripeService stripeService;
  private final UserPaymentRequestService userPaymentRequestService;
  private final UserBillingDiscountService userBillingDiscountService;

  @Override
  public void accept(UserMonthlyPaymentRequested userMonthlyPaymentRequested) {
    var userId = userMonthlyPaymentRequested.getUserId();
    var yearMonth = userMonthlyPaymentRequested.getYearMonth();
    var totalDiscountAmount = getTotalDiscountAmount(userId, yearMonth);
    var computedDueAmount = userMonthlyPaymentRequested.computeDueAmount();
    var enableAutomaticTaxes =
        computedDueAmount.convertCurrency(CENTS_EUR).amount().compareTo(totalDiscountAmount) > 0;

    log.info(
        "Creating invoice for userId={}; yearMonth={}; dueAmount={}; totalDiscount={};"
            + " enableAutoTaxing={}",
        userId,
        yearMonth,
        computedDueAmount,
        totalDiscountAmount,
        enableAutomaticTaxes);
    var draftInvoice =
        stripeService.createInvoice(
            userMonthlyPaymentRequested.getCustomerId(), enableAutomaticTaxes);
    updateInvoicePaymentDetails(userMonthlyPaymentRequested, draftInvoice, DRAFT);
    var invoiceId = draftInvoice.getId();
    userMonthlyPaymentRequested.getItemsToPay().stream()
        .filter(e -> !ZERO.equals(e.amount()))
        .forEach(e -> stripeCreateInvoiceItemWithUnitPrice(invoiceId, e));
    long totalDiscount = applyDiscountsToInvoice(userId, invoiceId, computedDueAmount, yearMonth);
    try {
      var finalizedInvoice = stripeService.finalizeInvoice(invoiceId);
      Long dueAmount = finalizedInvoice.getAmountDue();
      if (dueAmount != null && dueAmount == 0) {
        updateInvoicePaymentDetails(userMonthlyPaymentRequested, finalizedInvoice, totalDiscount);
        return;
      }
      var paidInvoice = stripeService.payInvoice(invoiceId);
      updateInvoicePaymentDetails(userMonthlyPaymentRequested, paidInvoice, totalDiscount);
    } catch (ApiException stripeException) {
      log.error("Error while processing payment", stripeException);
      var paymentRequest =
          userPaymentRequestService.getById(userMonthlyPaymentRequested.getPaymentRequestId());
      // Invoice finalization failed
      if (DRAFT.equals(paymentRequest.getInvoiceStatus())) {
        updateInvoicePaymentDetails(userMonthlyPaymentRequested, draftInvoice, REQUIRES_ACTION);
        return;
      }
      var paymentFailedInvoice = stripeService.retrieveInvoice(invoiceId);
      updateInvoicePaymentDetails(userMonthlyPaymentRequested, paymentFailedInvoice, totalDiscount);
    }
  }

  private void updateInvoicePaymentDetails(
      UserMonthlyPaymentRequested userMonthlyPaymentRequested,
      Invoice invoice,
      InvoiceStatus status) {
    UserPaymentRequest paymentRequest =
        userPaymentRequestService.getById(userMonthlyPaymentRequested.getPaymentRequestId());
    userPaymentRequestService.save(
        paymentRequest.toBuilder().invoiceId(invoice.getId()).invoiceStatus(status).build());
  }

  private void updateInvoicePaymentDetails(
      UserMonthlyPaymentRequested userMonthlyPaymentRequested,
      Invoice invoice,
      long appliedDiscount) {
    UserPaymentRequest paymentRequest =
        userPaymentRequestService.getById(userMonthlyPaymentRequested.getPaymentRequestId());
    InvoiceStatus invoicePaymentStatus = stripeService.getPaymentStatus(invoice);
    userPaymentRequestService.save(
        paymentRequest.toBuilder()
            .discountAmount(appliedDiscount)
            .invoiceId(invoice.getId())
            .invoiceUrl(invoice.getInvoicePdf())
            .invoiceStatus(invoicePaymentStatus)
            .build());
  }

  private void stripeCreateInvoiceItemWithAmount(
      String invoiceId, UserMonthlyPaymentRequested.ItemToPay domainItemToPay) {
    stripeService.createInvoiceItem(
        invoiceId,
        domainItemToPay.customerId(),
        domainItemToPay.amount(),
        domainItemToPay.description());
  }

  private void stripeCreateInvoiceItemWithUnitPrice(
      String invoiceId, UserMonthlyPaymentRequested.ItemToPay domainItemToPay) {
    stripeService.createInvoiceItem(
        invoiceId,
        domainItemToPay.customerId(),
        domainItemToPay.unitPrice(),
        domainItemToPay.quantity(),
        domainItemToPay.description());
  }

  private long stripeFreeTierAmount() {
    return pricingConf.freeTierAsMoney().convertCurrency(CENTS_EUR).amount().longValue();
  }

  private long applyDiscountsToInvoice(
      String userId, String invoiceId, Money dueAmount, YearMonth yearMonth) {
    long extraDiscountsAmount = getExtraDiscountsAmount(userId, yearMonth);
    var totalDiscountAmount = getTotalDiscountAmount(userId, yearMonth);

    var description =
        String.format(
            "Monthly 2€ discount %s", extraDiscountsAmount > 0 ? " + Extra discounts" : "");

    stripeService.applyDiscountToInvoice(invoiceId, description, totalDiscountAmount.longValue());

    return -(min(
        totalDiscountAmount.longValue(),
        dueAmount.convertCurrency(CENTS_EUR).amount().longValue()));
  }

  private BigDecimal getTotalDiscountAmount(String userId, YearMonth yearMonth) {
    long monthlyDiscountAmount = stripeFreeTierAmount();
    long extraDiscountsAmount = getExtraDiscountsAmount(userId, yearMonth);
    return BigDecimal.valueOf(monthlyDiscountAmount + extraDiscountsAmount);
  }

  private long getExtraDiscountsAmount(String userId, YearMonth yearMonth) {

    int year = yearMonth.getYear();
    var period = PaymentRequestPeriod.valueOf(yearMonth.getMonth().name());

    List<UserBillingDiscount> discounts =
        userBillingDiscountService.findAllByUserIdAndYearAndMonth(userId, year, period);

    if (discounts.isEmpty()) {
      return 0;
    }

    var totalDiscountAmount =
        new Money(
            discounts.stream()
                .map(UserBillingDiscount::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .max(BigDecimal.ZERO),
            EUR);

    if (totalDiscountAmount.amount().compareTo(BigDecimal.ZERO) == 0) {
      return 0;
    }

    return totalDiscountAmount.convertCurrency(CENTS_EUR).amount().longValue();
  }
}
