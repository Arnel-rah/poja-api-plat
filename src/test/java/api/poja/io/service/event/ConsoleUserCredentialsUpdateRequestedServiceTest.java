package api.poja.io.service.event;

import static api.poja.io.integration.conf.utils.TestMocks.ADMIN_ID;
import static api.poja.io.integration.conf.utils.TestMocks.JOE_DOE_ID;
import static api.poja.io.integration.conf.utils.TestMocks.JOE_DOE_MAIN_ORG_ID;
import static api.poja.io.integration.conf.utils.TestMocks.LOREM_IPSUM_ID;
import static api.poja.io.integration.conf.utils.TestMocks.NOOBIE_ID;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import api.poja.io.conf.MockedThirdParties;
import api.poja.io.endpoint.event.model.ConsoleUserCredentialsUpdateEmailNotificationRequested;
import api.poja.io.endpoint.event.model.ConsoleUserCredentialsUpdateRequested;
import api.poja.io.endpoint.event.model.PojaEvent;
import api.poja.io.service.ConsoleUserService;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

public class ConsoleUserCredentialsUpdateRequestedServiceTest extends MockedThirdParties {
  @Autowired private ConsoleUserCredentialsUpdateRequestedService subject;
  @MockBean private ConsoleUserService consoleUserServiceMock;

  private static final String ORG_ID = JOE_DOE_MAIN_ORG_ID;

  @Test
  void orgConsoleUserCredentialsUpdate_notifies_orgMembers() {
    subject.accept(new ConsoleUserCredentialsUpdateRequested(ORG_ID));

    verify(consoleUserServiceMock, times(1)).updateConsoleUserPassword(ORG_ID);

    verify(eventProducerMock, times(1))
        .accept(
            argThat(ConsoleUserCredentialsUpdateRequestedServiceTest::onlyOrgMembers_areNotified));
  }

  private static boolean onlyOrgMembers_areNotified(Collection<? extends PojaEvent> events) {
    var expected = expectedConsoleUserCredentialsUpdateRequestedEvents();
    return events.size() == expected.size() && events.containsAll(expected);
  }

  private static List<? extends PojaEvent> expectedConsoleUserCredentialsUpdateRequestedEvents() {
    return Stream.of(JOE_DOE_ID, NOOBIE_ID, ADMIN_ID, LOREM_IPSUM_ID)
        .map(id -> new ConsoleUserCredentialsUpdateEmailNotificationRequested(ORG_ID, id))
        .toList();
  }
}
