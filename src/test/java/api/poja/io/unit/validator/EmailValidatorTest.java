package api.poja.io.unit.validator;

import static api.poja.io.integration.conf.utils.TestUtils.assertThrowsDomainBadRequestException;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import api.poja.io.service.validator.EmailValidator;
import org.junit.jupiter.api.Test;

class EmailValidatorTest {
  private final EmailValidator subject = new EmailValidator();
  private static final String INVALID_EMAIL = "invalid-email";
  private static final String JOE_DOE_EMAIL = "joedoe@gmail.com";

  @Test
  void validate_null_or_invalid() {
    assertThrowsDomainBadRequestException(
        "Email is not a valid email address.", () -> subject.accept(null));
    assertThrowsDomainBadRequestException(
        "Email is not a valid email address.", () -> subject.accept(INVALID_EMAIL));
    assertDoesNotThrow(() -> subject.accept(JOE_DOE_EMAIL));
  }

  @Test
  void test_null_or_invalid() {
    assertFalse(subject.test(null));
    assertFalse(subject.test(INVALID_EMAIL));
    assertTrue(subject.test(JOE_DOE_EMAIL));
  }
}
