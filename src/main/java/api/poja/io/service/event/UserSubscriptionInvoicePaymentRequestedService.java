package api.poja.io.service.event;

import static api.poja.io.repository.model.enums.InvoiceStatus.UNKNOWN;

import api.poja.io.endpoint.event.model.UserSubscriptionInvoicePaymentRequested;
import api.poja.io.model.User;
import api.poja.io.repository.model.Invoice;
import api.poja.io.repository.model.Offer;
import api.poja.io.repository.model.enums.InvoiceStatus;
import api.poja.io.service.InvoiceService;
import api.poja.io.service.UserService;
import api.poja.io.service.UserSubscriptionService;
import api.poja.io.service.stripe.StripeService;
import api.poja.io.sys.platform.SaasOnly;
import java.time.Instant;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
@SaasOnly
public class UserSubscriptionInvoicePaymentRequestedService
    implements Consumer<UserSubscriptionInvoicePaymentRequested> {
  private final UserService userService;
  private final UserSubscriptionService userSubscriptionService;
  private final InvoiceService invoiceService;
  private final StripeService stripeService;

  @Override
  public void accept(UserSubscriptionInvoicePaymentRequested event) {
    var subscription =
        userSubscriptionService.getByUserIdAndId(event.getUserId(), event.getUserSubscriptionId());
    var invoice = subscription.getInvoice();
    InvoiceStatus invoiceStatus = invoice.getStatus();
    if (!UNKNOWN.equals(invoiceStatus) && !invoiceStatus.canBePaid()) {
      return;
    }
    if (invoice.getInvoiceId() != null) {
      invoiceService.payInvoice(invoice);
      return;
    }
    User user = userService.getUserById(event.getUserId());
    createAndPay(user, subscription.getInvoice(), subscription.getOffer(), event.getRequestedAt());
  }

  private Invoice createAndPay(User user, Invoice invoice, Offer offer, Instant requestedAt) {
    var stripeInvoice = stripeService.createInvoice(user.getStripeId(), false);
    invoiceService.refreshInvoice(invoice, stripeInvoice);
    String stripeInvoiceId = stripeInvoice.getId();
    stripeService.createInvoiceItem(
        stripeInvoiceId,
        user.getStripeId(),
        offer.getMonthlyPriceAsMoney(),
        "subscription to poja-" + offer.getName() + "of date " + requestedAt);
    var finalizedInvoice = stripeService.finalizeInvoice(stripeInvoiceId);
    return pay(invoice, finalizedInvoice);
  }

  private Invoice pay(Invoice invoice, com.stripe.model.Invoice stripeInvoice) {
    Long dueAmount = stripeInvoice.getAmountDue();
    String domainInvoiceId = invoice.getId();
    if (dueAmount != null && dueAmount == 0) {
      return updateInvoicePaymentDetails(domainInvoiceId, stripeInvoice);
    }
    return invoiceService.payInvoice(invoiceService.refreshInvoice(invoice, stripeInvoice));
  }

  private Invoice updateInvoicePaymentDetails(
      String domainInvoiceId, com.stripe.model.Invoice stripeInvoice) {
    var domainInvoice = invoiceService.getById(domainInvoiceId);
    InvoiceStatus invoicePaymentStatus = stripeService.getPaymentStatus(stripeInvoice);
    return invoiceService.save(
        domainInvoice.toBuilder()
            .invoiceId(stripeInvoice.getId())
            .invoiceUrl(stripeInvoice.getInvoicePdf())
            .status(invoicePaymentStatus)
            .build());
  }
}
