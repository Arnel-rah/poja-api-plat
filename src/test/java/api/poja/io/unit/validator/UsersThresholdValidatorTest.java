package api.poja.io.unit.validator;

import static api.poja.io.integration.conf.utils.TestUtils.assertThrowsDomainServiceUnavailableException;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import api.poja.io.model.OfferDto;
import api.poja.io.repository.UserRepository;
import api.poja.io.service.OfferService;
import api.poja.io.service.symjaService.SymjaService;
import api.poja.io.service.validator.UsersThresholdValidator;
import org.junit.jupiter.api.Test;

public class UsersThresholdValidatorTest {
  private static final int MAX_PREMIUM_USERS_NB = 10;
  private static final long CURRENT_USERS_NB = 1L;
  public static final long PREMIUM_USERS_APP_NB = 12L;

  private UsersThresholdValidator thresholdValidator() {
    var userRepository = mock(UserRepository.class);
    when(userRepository.countAll()).thenReturn(CURRENT_USERS_NB);
    var offerService = mock(OfferService.class);
    when(offerService.getPremiumOffer())
        .thenReturn(new OfferDto(null, "premium", PREMIUM_USERS_APP_NB, null, 0, 0));
    var symjaService = new SymjaService(offerService);
    return new UsersThresholdValidator(MAX_PREMIUM_USERS_NB, userRepository, symjaService);
  }

  @Test
  void validate_threshold_ok() {
    long usersToCreate = 20L;

    assertDoesNotThrow(() -> thresholdValidator().accept(usersToCreate));
  }

  @Test
  void validate_threshold_ko() {
    long usersToCreate = 450L;

    assertThrowsDomainServiceUnavailableException(
        "Cannot add "
            + usersToCreate
            + " users: this would exceed the maximum limit of 450 users (current users: "
            + CURRENT_USERS_NB
            + ").",
        () -> thresholdValidator().accept(usersToCreate));
  }
}
