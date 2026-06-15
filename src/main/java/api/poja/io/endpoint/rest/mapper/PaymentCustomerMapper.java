package api.poja.io.endpoint.rest.mapper;

import api.poja.io.endpoint.rest.model.Address;
import api.poja.io.endpoint.rest.model.PaymentCustomer;
import api.poja.io.endpoint.rest.model.PaymentMethod;
import api.poja.io.service.stripe.StripeService;
import com.stripe.model.Customer;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class PaymentCustomerMapper {
  private final StripeService stripeService;
  private final PaymentMapper paymentMapper;

  public PaymentCustomer toRest(Customer domain) {
    var invoiceSettings = domain.getInvoiceSettings();
    var defaultPaymentMethod =
        invoiceSettings.getDefaultPaymentMethod() != null
            ? getDefaultPaymentMethod(invoiceSettings.getDefaultPaymentMethod())
            : null;
    var address = getAddress(domain.getAddress(), defaultPaymentMethod);

    return new PaymentCustomer()
        .id(domain.getId())
        .name(domain.getName())
        .email(domain.getEmail())
        .phone(domain.getPhone())
        .address(address)
        .defaultPaymentMethod(defaultPaymentMethod);
  }

  private Address getAddress(com.stripe.model.Address domainAddress, PaymentMethod paymentMethod) {
    if (domainAddress != null) {
      return paymentMapper.toBillingAddress(domainAddress);
    }
    if (paymentMethod != null) {
      return paymentMethod.getBillingAddress();
    }
    return null;
  }

  private PaymentMethod getDefaultPaymentMethod(String paymentMethodId) {
    return paymentMapper.toRest(stripeService.retrievePaymentMethod(paymentMethodId));
  }
}
