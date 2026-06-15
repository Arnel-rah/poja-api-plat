package api.poja.io.symja;

import static api.poja.io.unit.validator.UsersThresholdValidatorTest.PREMIUM_USERS_APP_NB;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import api.poja.io.model.OfferDto;
import api.poja.io.service.OfferService;
import api.poja.io.service.symjaService.SymjaService;
import org.junit.jupiter.api.Test;

class SymjaTest {
  SymjaService subject = symjaService();

  private static SymjaService symjaService() {
    var offerService = mock(OfferService.class);
    var premiumOffer = new OfferDto(null, "premium", PREMIUM_USERS_APP_NB, null, 0, 0);
    when(offerService.getPremiumOffer()).thenReturn(premiumOffer);
    return new SymjaService(offerService);
  }

  @Test
  void computeMaxBasicUsersGivenPremium() {
    var case10 = subject.computeMaxBasicUsersGivenPremium(10);
    var case20 = subject.computeMaxBasicUsersGivenPremium(20);
    var case40 = subject.computeMaxBasicUsersGivenPremium(40);
    var case50 = subject.computeMaxBasicUsersGivenPremium(50);

    assertEquals(440, case10.intValue());
    assertEquals(380, case20.intValue());
    assertEquals(260, case40.intValue());
    assertEquals(200, case50.intValue());
    assertThrows(
        IllegalArgumentException.class, () -> subject.computeMaxBasicUsersGivenPremium(100));
  }

  @Test
  void computeNeededLogPolicies() {
    var case12And3 = subject.computeNeededLogPolicies(12, 3);
    var case1And1 = subject.computeNeededLogPolicies(1, 1);

    assertEquals(6, case12And3.intValue());
    assertEquals(1, case1And1.intValue());
  }

  @Test
  void computeMaxConsoleUserGroups() {
    var case6And3 = subject.computeMaxConsoleUserGroups(6, 3);

    assertEquals(6, case6And3.intValue());
  }
}
