package api.poja.io.endpoint.validator;

import static api.poja.io.integration.conf.utils.TestUtils.assertThrowsDomainBadRequestException;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import api.poja.io.endpoint.rest.model.CrupdatePojaVersionChangelogRequestBody;
import org.junit.jupiter.api.Test;

class CrupdatePojaVersionChangelogValidatorTest {
  private final CrupdatePojaVersionChangelogValidator subject =
      new CrupdatePojaVersionChangelogValidator();

  @Test
  void accept_ko() {
    assertThrowsDomainBadRequestException(
        "pojaVersion must not be null or empty",
        () ->
            subject.accept(
                null, new CrupdatePojaVersionChangelogRequestBody().changelogMd("release 3.0.0")));

    assertThrowsDomainBadRequestException(
        "changelog must not be null", () -> subject.accept("3.0.0", null));
  }

  @Test
  void accept_ok() {
    assertDoesNotThrow(
        () ->
            subject.accept(
                "3.0.0",
                new CrupdatePojaVersionChangelogRequestBody().changelogMd("release 3.0.0")));
  }
}
