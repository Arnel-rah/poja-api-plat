package api.poja.io.service;

import static api.poja.io.integration.conf.utils.TestMocks.DENIS_RITCHIE_ID;
import static api.poja.io.integration.conf.utils.TestMocks.NOOBIE_ID;
import static api.poja.io.integration.conf.utils.TestMocks.PREMIUM_OFFER_ID;
import static api.poja.io.repository.model.enums.InvoiceStatus.PAID;
import static api.poja.io.repository.model.enums.InvoiceStatus.UNKNOWN;
import static java.time.Instant.now;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;

import api.poja.io.conf.MockedThirdParties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class UserSubscriptionServiceTest extends MockedThirdParties {
  @Autowired UserSubscriptionService subject;

  static final boolean MANUAL_SUBSCRIPTION = true;

  @Test
  void should_subscribe_user_afterPayment_for_saas() {
    final String userId = NOOBIE_ID;
    doReturn(true).when(platformConfSpy).isSaas();
    doReturn(false).when(platformConfSpy).isIdp();

    var userSub = subject.subscribe(userId, PREMIUM_OFFER_ID, now(), MANUAL_SUBSCRIPTION);

    assertEquals(UNKNOWN, userSub.getInvoice().getStatus());
    assertFalse(userSub.isActive());

    subject.unsubscribe(userId, userSub.getId());
  }

  @Test
  void should_subscribe_user_instantly_with_far_in_future_end_datetime_for_idp() {
    final String userId = DENIS_RITCHIE_ID;
    doReturn(true).when(platformConfSpy).isIdp();
    doReturn(false).when(platformConfSpy).isSaas();

    var userSub = subject.subscribe(userId, PREMIUM_OFFER_ID, now(), MANUAL_SUBSCRIPTION);

    assertEquals(PAID, userSub.getInvoice().getStatus());
    assertTrue(userSub.isActive());

    subject.unsubscribe(userId, userSub.getId());
  }
}
