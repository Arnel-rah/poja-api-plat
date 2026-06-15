package api.poja.io.service.validator;

import static api.poja.io.integration.conf.utils.TestUtils.assertThrowsDomainBadRequestException;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import api.poja.io.endpoint.rest.model.Address;
import org.junit.jupiter.api.Test;

class AddressValidatorTest {
  private final AddressValidator subject = new AddressValidator();

  @Test
  void validAddress_shouldPass() {
    var validAddress =
        new Address()
            .country("dummy")
            .city("dummy")
            .state("dummy")
            .postalCode("dummy")
            .line1("dummy")
            .line2("dummy");

    assertDoesNotThrow(() -> subject.accept(validAddress));
  }

  @Test
  void nullAddress_shouldPass() {
    assertDoesNotThrow(() -> subject.accept(null));
  }

  @Test
  void nonNullAddressWithNullProperties_shouldFail() {
    var invalidAddress = new Address();

    assertThrowsDomainBadRequestException(
        "address.country is mandatory. address.city is mandatory. address.postalCode is mandatory."
            + " address.line1 is mandatory",
        () -> subject.accept(invalidAddress));
  }
}
