package api.poja.io.service.event;

import static java.util.UUID.randomUUID;

import api.poja.io.endpoint.event.model.UserMainOrganizationSetupRequested;
import api.poja.io.endpoint.rest.model.Organization;
import api.poja.io.service.UserService;
import api.poja.io.service.organization.OrganizationService;
import java.util.List;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@AllArgsConstructor
public class UserMainOrganizationSetupRequestedService
    implements Consumer<UserMainOrganizationSetupRequested> {
  private final OrganizationService organizationService;
  private final UserService userService;

  @Override
  public void accept(UserMainOrganizationSetupRequested userMainOrganizationSetupRequested) {
    var user = userMainOrganizationSetupRequested.getUser();

    log.info("Create main org for user id {}", user.getId());
    var createdOrg =
        organizationService
            .crupdateOrgs(
                user.getId(),
                List.of(
                    new Organization()
                        .id(randomUUID().toString())
                        .name("org-" + user.getUsername())))
            .getFirst();
    userService.updateMainOrgId(user.getId(), createdOrg.id());
  }
}
