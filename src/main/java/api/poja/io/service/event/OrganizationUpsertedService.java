package api.poja.io.service.event;

import static api.poja.io.repository.model.enums.OrganizationSetupStateStatusEnum.ORGANIZATION_SETUP_COMPLETED;
import static api.poja.io.repository.model.enums.OrganizationSetupStateStatusEnum.ORGANIZATION_SETUP_FAILED;
import static api.poja.io.repository.model.enums.OrganizationSetupStateStatusEnum.ORGANIZATION_SETUP_IN_PROGRESS;
import static java.util.UUID.randomUUID;

import api.poja.io.endpoint.event.model.OrganizationUpserted;
import api.poja.io.repository.model.ConsoleUserGroup;
import api.poja.io.repository.model.Organization;
import api.poja.io.service.ConsoleUserGroupService;
import api.poja.io.service.ConsoleUserService;
import api.poja.io.service.OrganizationSetupStateService;
import api.poja.io.service.organization.OrganizationService;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.iam.model.IamException;
import software.amazon.awssdk.services.iam.model.LimitExceededException;

@Service
@AllArgsConstructor
@Slf4j
public class OrganizationUpsertedService implements Consumer<OrganizationUpserted> {
  private final ConsoleUserGroupService consoleUserGroupService;
  private final ConsoleUserService consoleUserService;
  private final OrganizationService organizationService;
  private final OrganizationSetupStateService organizationSetupStateService;
  public static final String POJA_USER_GROUP_NAME_PREFIX = "poja-group-";

  @Override
  public void accept(OrganizationUpserted organizationUpserted) {
    var org = organizationUpserted.getOrganization();
    organizationSetupStateService.save(org.getId(), ORGANIZATION_SETUP_IN_PROGRESS);
    try {
      var consoleCredentials = consoleUserService.createConsoleUser(org.getId());
      createUserGroup(org, consoleCredentials.username());
      organizationSetupStateService.save(org.getId(), ORGANIZATION_SETUP_COMPLETED);
    } catch (IamException e) {
      organizationSetupStateService.save(org.getId(), ORGANIZATION_SETUP_FAILED);
      log.error("e", e);
    }
  }

  private void createUserGroup(Organization org, String consoleUsername)
      throws LimitExceededException {
    var savedGroup =
        consoleUserGroupService.createNewByUser(
            org.getOwnerId(),
            consoleUsername,
            ConsoleUserGroup.builder()
                .name(POJA_USER_GROUP_NAME_PREFIX + randomUUID().toString().substring(0, 8))
                .available(true)
                .archived(false)
                .orgId(org.getId())
                .userId(org.getOwnerId())
                .build());
    var policyDocumentName = "org-" + org.getId() + "-group-logPolicies";
    organizationService.updateConsoleInformations(
        org.getId(), savedGroup.getName(), policyDocumentName);
  }
}
