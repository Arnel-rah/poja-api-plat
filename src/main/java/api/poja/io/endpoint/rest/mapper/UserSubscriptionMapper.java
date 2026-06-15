package api.poja.io.endpoint.rest.mapper;

import static api.poja.io.model.money.Currency.CENTS_EUR;

import api.poja.io.endpoint.rest.model.Invoice;
import api.poja.io.endpoint.rest.model.InvoiceStatusEnum;
import api.poja.io.endpoint.rest.model.UserSubscription;
import org.springframework.stereotype.Component;

@Component
public class UserSubscriptionMapper {
  public UserSubscription toRest(api.poja.io.repository.model.UserSubscription domain) {
    return new UserSubscription()
        .id(domain.getId())
        .offerId(domain.getOffer().getId())
        .userId(domain.getUserId())
        .invoice(toRestInvoice(domain.getInvoice()))
        .subscriptionBeginDatetime(domain.getSubscriptionBeginDatetime())
        .subscriptionEndDatetime(domain.getSubscriptionEndDatetime())
        .isActive(domain.isActive());
  }

  private Invoice toRestInvoice(api.poja.io.repository.model.Invoice domain) {
    return new Invoice()
        .id(domain.getId())
        .invoiceId(domain.getInvoiceId())
        .invoiceStatus(InvoiceStatusEnum.valueOf(domain.getStatus().name()))
        .invoiceUrl(domain.getInvoiceUrl())
        .amount(domain.getAmountAsMoney().convertCurrency(CENTS_EUR).amount().intValue())
        .discountAmount(0);
  }
}
