package api.poja.io.service.event;

import static java.time.Month.APRIL;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import api.poja.io.endpoint.event.EventProducer;
import api.poja.io.endpoint.event.model.RefreshUserCostRequested;
import api.poja.io.endpoint.event.model.RefreshUsersCostTriggered;
import api.poja.io.repository.jpa.UserJpaRepository;
import api.poja.io.service.UserService;
import java.time.LocalDate;
import java.time.YearMonth;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;

class RefreshUsersCostTriggeredServiceTest {
  UserService userServiceMock = mock();
  EventProducer<RefreshUserCostRequested> eventProducerMock = mock();
  RefreshUsersCostTriggeredService subject =
      new RefreshUsersCostTriggeredService(userServiceMock, eventProducerMock);

  @MockBean private UserJpaRepository userJpaRepository;

  @Test
  void accept() {
    var date = LocalDate.of(2025, APRIL, 17);
    var yearMonth = YearMonth.from(date);

    subject.accept(new RefreshUsersCostTriggered(date));

    verify(userServiceMock, times(1)).findAllToBill(yearMonth);
    verify(eventProducerMock, times(1)).accept(anyCollection());
    verifyNoMoreInteractions(userServiceMock, eventProducerMock);
  }
}
