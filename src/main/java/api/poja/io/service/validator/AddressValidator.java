package api.poja.io.service.validator;

import api.poja.io.endpoint.rest.model.Address;
import api.poja.io.model.exception.BadRequestException;
import java.util.StringJoiner;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;

@Component
public class AddressValidator implements Consumer<Address> {

  @Override
  public void accept(Address address) {
    if (address == null) {
      return;
    }
    var exceptionMessage = new StringJoiner(". ");

    if (address.getCountry() == null) {
      exceptionMessage.add("address.country is mandatory");
    }
    if (address.getCity() == null) {
      exceptionMessage.add("address.city is mandatory");
    }
    if (address.getPostalCode() == null) {
      exceptionMessage.add("address.postalCode is mandatory");
    }
    if (address.getLine1() == null) {
      exceptionMessage.add("address.line1 is mandatory");
    }

    if (exceptionMessage.length() > 0) {
      throw new BadRequestException(exceptionMessage.toString());
    }
  }
}
