package api.poja.io.service.event;

import api.poja.io.endpoint.event.EventProducer;
import api.poja.io.endpoint.event.model.ConsoleUserCredentialsUpdateEmailNotificationRequested;
import api.poja.io.endpoint.event.model.ConsoleUserCredentialsUpdateRequested;
import api.poja.io.endpoint.event.model.PojaEvent;
import api.poja.io.service.ConsoleUserService;
import api.poja.io.service.organization.OrganizationUsersService;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.iam.model.IamException;

@Service
@AllArgsConstructor
@Slf4j
public class ConsoleUserCredentialsUpdateRequestedService
    implements Consumer<ConsoleUserCredentialsUpdateRequested> {
  private final ConsoleUserService consoleUserService;
  private final OrganizationUsersService organizationUsersService;
  private final EventProducer<PojaEvent> eventProducer;

  @Override
  public void accept(ConsoleUserCredentialsUpdateRequested consoleUserCredentialsUpdateRequested) {
    var orgId = consoleUserCredentialsUpdateRequested.getOrgId();
    try {
      log.info("Updating orgId={} console user credentials", orgId);
      consoleUserService.updateConsoleUserPassword(orgId);
    } catch (IamException e) {
      log.error("Failed to update console user credentials password", e);
      return;
    }
    log.info(
        "Sending email notification to orgId={} members for console user credentials update",
        orgId);
    var emailEvents =
        organizationUsersService.getOrgMembers(orgId).stream()
            .map(u -> toEmailRequestedEvent(orgId, u.getId()))
            .toList();
    eventProducer.accept(emailEvents);
  }

  private static PojaEvent toEmailRequestedEvent(String orgId, String userId) {
    return new ConsoleUserCredentialsUpdateEmailNotificationRequested(orgId, userId);
  }
}
