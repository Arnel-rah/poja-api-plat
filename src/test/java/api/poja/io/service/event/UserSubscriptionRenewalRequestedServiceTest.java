package api.poja.io.service.event;

import static api.poja.io.integration.conf.utils.TestMocks.JOE_DOE_ID;
import static api.poja.io.integration.conf.utils.TestMocks.PREMIUM_OFFER_ID;
import static java.time.YearMonth.of;
import static java.time.ZoneOffset.UTC;
import static java.time.temporal.ChronoUnit.MILLIS;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import api.poja.io.conf.MockedThirdParties;
import api.poja.io.endpoint.event.model.UserSubscriptionRenewalRequested;
import api.poja.io.endpoint.event.model.UsersSubscriptionRenewalRequested;
import api.poja.io.repository.model.Offer;
import api.poja.io.repository.model.UserSubscription;
import api.poja.io.service.UserSubscriptionService;
import java.time.YearMonth;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;

class UserSubscriptionRenewalRequestedServiceTest extends MockedThirdParties {
  private static final YearMonth YEAR_MONTH = of(2020, 1);

  @Autowired
  private UsersSubscriptionRenewalRequestedService usersSubscriptionRenewalRequestedService;

  @Autowired
  private UserSubscriptionRenewalRequestedService userSubscriptionRenewalRequestedService;

  @SpyBean UserSubscriptionService userSubscriptionServiceSpy;

  private static UsersSubscriptionRenewalRequested usersSubscriptionRenewalRequested() {
    return UsersSubscriptionRenewalRequested.builder().yearMonth(YEAR_MONTH).build();
  }

  private static UserSubscription activeUserSubscription() {
    return UserSubscription.builder()
        .userId(JOE_DOE_ID)
        .offer(Offer.builder().id(PREMIUM_OFFER_ID).name("premium").build())
        .build();
  }

  @Test
  void find_all_to_renew_on_trigger_year_month() {
    usersSubscriptionRenewalRequestedService.accept(usersSubscriptionRenewalRequested());
    verify(userSubscriptionServiceSpy, times(1)).findAllToRenew(YEAR_MONTH);
    verify(eventProducerMock, times(1)).accept(anyList());
  }

  @Test
  void subscribe_to_incoming_month() {
    var currentSub = activeUserSubscription();

    userSubscriptionRenewalRequestedService.accept(
        UserSubscriptionRenewalRequested.builder()
            .parentEvent(usersSubscriptionRenewalRequested())
            .userCurrentSubscription(currentSub)
            .build());

    boolean isManualSubscription = false;
    verify(userSubscriptionServiceSpy, times(1))
        .subscribe(
            currentSub.getUserId(),
            currentSub.getOffer().getId(),
            YEAR_MONTH.plusMonths(1).atDay(1).atStartOfDay(UTC).toInstant().truncatedTo(MILLIS),
            isManualSubscription);
  }
}
