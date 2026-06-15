package api.poja.io.endpoint.rest.mapper;

import api.poja.io.endpoint.rest.model.PaymentMethod;
import com.stripe.model.Address;
import org.springframework.stereotype.Component;

@Component
public class PaymentMapper {
  public PaymentMethod toRest(com.stripe.model.PaymentMethod domain) {
    com.stripe.model.PaymentMethod.Card card = domain.getCard();
    var billingAddress = toBillingAddress(domain.getBillingDetails().getAddress());
    return new PaymentMethod()
        .id(domain.getId())
        .expMonth(Math.toIntExact(card.getExpMonth()))
        .expYear(Math.toIntExact(card.getExpYear()))
        .type(domain.getType())
        .brand(card.getBrand())
        .last4(card.getLast4())
        .billingAddress(billingAddress);
  }

  public api.poja.io.endpoint.rest.model.Address toBillingAddress(Address domain) {
    return new api.poja.io.endpoint.rest.model.Address()
        .city(domain.getCity())
        .country(domain.getCountry())
        .line1(domain.getLine1())
        .line2(domain.getLine2())
        .postalCode(domain.getPostalCode());
  }
}
