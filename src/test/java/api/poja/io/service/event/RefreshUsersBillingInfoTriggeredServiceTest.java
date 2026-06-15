package api.poja.io.service.event;

import static java.time.Month.APRIL;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import api.poja.io.conf.MockedThirdParties;
import api.poja.io.endpoint.event.model.RefreshUsersBillingInfoTriggered;
import api.poja.io.repository.jpa.UserJpaRepository;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

class RefreshUsersBillingInfoTriggeredServiceTest extends MockedThirdParties {
  @Autowired private RefreshUsersBillingInfoTriggeredService subject;
  @MockBean private UserJpaRepository userJpaRepository;

  @Test
  void accept() {
    when(platformConfSpy.isSaas()).thenReturn(true);
    var localDate = LocalDate.of(2025, APRIL, 17);
    var endTime = Instant.parse("2025-04-17T00:00:00.000Z");

    subject.accept(
        RefreshUsersBillingInfoTriggered.builder()
            .utcLocalDate(localDate)
            .pricingCalculationRequestEndTime(endTime)
            .build());

    verify(userJpaRepository, times(1)).findAllToComputeBilling(endTime, localDate.minusDays(1));
    verify(eventProducerMock, times(1)).accept(anyCollection());
  }
}
