package api.poja.io.service;

import static api.poja.io.repository.model.enums.InvoiceStatus.CANCELED;
import static api.poja.io.repository.model.enums.InvoiceStatus.DRAFT;
import static api.poja.io.repository.model.enums.InvoiceStatus.OPEN;
import static java.util.Optional.empty;

import api.poja.io.endpoint.rest.model.PayInvoice;
import api.poja.io.model.exception.ApiException;
import api.poja.io.model.exception.BadRequestException;
import api.poja.io.model.exception.NotFoundException;
import api.poja.io.repository.jpa.InvoiceRepository;
import api.poja.io.repository.model.Invoice;
import api.poja.io.repository.model.enums.InvoiceStatus;
import api.poja.io.service.stripe.StripeService;
import java.util.Optional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class InvoiceService {
  private final InvoiceRepository repository;
  private final StripeService stripeService;

  public Invoice save(Invoice invoice) {
    return repository.save(invoice);
  }

  public Optional<Invoice> findById(String id) {
    return repository.findById(id);
  }

  public Invoice getById(String id) {
    return findById(id)
        .orElseThrow(() -> new NotFoundException("Invoice.Id=" + id + " was not found."));
  }

  public Invoice getByUserIdAndId(String userId, String id) {
    return repository
        .findByUserIdAndId(userId, id)
        .orElseThrow(
            () ->
                new NotFoundException(
                    "Invoice.Id=" + id + " UserId=" + userId + " was not found."));
  }

  public Invoice payInvoice(String userId, PayInvoice payInvoice) {
    var invoice = getByUserIdAndId(userId, payInvoice.getInvoiceId());
    return payInvoice(invoice, Optional.ofNullable(payInvoice.getPaymentMethodId()));
  }

  public Invoice payInvoice(Invoice invoice, Optional<String> paymentMethodId) {
    var refreshedInvoice = refreshInvoice(invoice);
    if (!refreshedInvoice.canBePaid()) {
      throw new BadRequestException(
          "Invoice.Id=" + invoice.getId() + " is already at status " + invoice.getStatus());
    }
    if (DRAFT.equals(refreshedInvoice.getStatus())) {
      stripeService.finalizeInvoice(refreshedInvoice.getInvoiceId());
    }
    String stripeInvoiceId = invoice.getInvoiceId();
    try {
      var paidInvoice =
          paymentMethodId.isEmpty()
              ? stripeService.payInvoice(stripeInvoiceId)
              : stripeService.payInvoice(stripeInvoiceId, paymentMethodId.get());
      return updateInvoicePaymentDetails(invoice.getId(), paidInvoice);
    } catch (ApiException stripeException) {
      var paymentFailedInvoice = stripeService.retrieveInvoice(stripeInvoiceId);
      return updateInvoicePaymentDetails(invoice.getId(), paymentFailedInvoice);
    }
  }

  public Invoice payInvoice(Invoice invoice) {
    return payInvoice(invoice, empty());
  }

  public Invoice refreshInvoice(Invoice invoice) {
    if (invoice.getInvoiceId() == null) {
      return invoice;
    }
    var stripeInvoice = stripeService.retrieveInvoice(invoice.getInvoiceId());
    if (InvoiceStatus.fromValue(stripeInvoice.getStatus()).equals(OPEN)) {
      return refreshInvoice(invoice, stripeInvoice);
    }
    InvoiceStatus paymentStatus = stripeService.getPaymentStatus(stripeInvoice);
    invoice.setStatus(paymentStatus);
    return invoice;
  }

  public Invoice refreshInvoice(Invoice invoice, com.stripe.model.Invoice stripeInvoice) {
    invoice.setInvoiceUrl(stripeInvoice.getInvoicePdf());
    invoice.setInvoiceId(stripeInvoice.getId());
    invoice.setStatus(InvoiceStatus.fromValue(stripeInvoice.getStatus()));
    return invoice;
  }

  private Invoice updateInvoicePaymentDetails(
      String domainInvoiceId, com.stripe.model.Invoice stripeInvoice) {
    var domainInvoice = getById(domainInvoiceId);
    InvoiceStatus invoicePaymentStatus = stripeService.getPaymentStatus(stripeInvoice);
    return save(
        domainInvoice.toBuilder()
            .invoiceId(stripeInvoice.getId())
            .invoiceUrl(stripeInvoice.getInvoicePdf())
            .status(invoicePaymentStatus)
            .build());
  }

  public Invoice voidInvoice(Invoice invoice) {
    String invoiceId = invoice.getInvoiceId();
    if (invoiceId == null) {
      return save(invoice.toBuilder().status(CANCELED).build());
    }
    stripeService.voidInvoice(invoiceId);
    return refreshInvoice(invoice);
  }
}
