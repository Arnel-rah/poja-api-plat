package api.poja.io.service.event;

import api.poja.io.aws.iam.IamComponent;
import api.poja.io.endpoint.event.model.ConsoleUserStatusUpdateRequested;
import api.poja.io.model.User;
import api.poja.io.repository.model.Organization;
import api.poja.io.service.UserService;
import api.poja.io.service.organization.OrganizationService;
import api.poja.io.sys.platform.SaasOnly;
import java.util.List;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@SaasOnly
public class ConsoleUserStatusUpdateRequestedService
    implements Consumer<ConsoleUserStatusUpdateRequested> {
  private final IamComponent iamComponent;
  private final UserService userService;
  private final OrganizationService organizationService;

  @Override
  public void accept(ConsoleUserStatusUpdateRequested consoleUserStatusUpdateRequested) {
    User user = userService.getUserById(consoleUserStatusUpdateRequested.getUserId());
    List<Organization> ownedOrganizations = organizationService.getAllByOwnerId(user.getId());
    ownedOrganizations.forEach(
        organization ->
            updateIamStatus(
                consoleUserStatusUpdateRequested.getStatus(), organization.getConsoleUsername()));
  }

  private void updateIamStatus(
      ConsoleUserStatusUpdateRequested.StatusAlteration status, String consoleUsername) {
    switch (status) {
      case SUSPEND -> iamComponent.suspendUser(consoleUsername);
      case ACTIVATE -> iamComponent.enableUser(consoleUsername);
    }
  }
}
