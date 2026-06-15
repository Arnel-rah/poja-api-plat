package api.poja.io.service;

import api.poja.io.aws.iam.IamComponent;
import api.poja.io.aws.iam.model.ConsoleUserCredentials;
import api.poja.io.model.exception.NotFoundException;
import api.poja.io.service.organization.OrganizationService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.iam.model.IamException;

@Service
@AllArgsConstructor
public class ConsoleUserService {
  private final IamComponent iamComponent;
  private final OrganizationService organizationService;

  public ConsoleUserCredentials createConsoleUser(String orgId) throws IamException {
    organizationService.getById(orgId); // ensure user is in db
    var consoleUsername = "poja-org-" + orgId;
    var consoleUserCredentials = iamComponent.createIam(consoleUsername);
    var policyDocumentName = consoleUserCredentials.username() + "-user-logPolicies";
    organizationService.updateConsoleCredentials(orgId, consoleUserCredentials, policyDocumentName);
    return consoleUserCredentials;
  }

  public ConsoleUserCredentials getConsoleUser(String orgId) {
    var organization = organizationService.getById(orgId);
    if (organization.getConsoleUsername() == null) {
      throw new NotFoundException("Console User with OrgId = " + orgId + " not found.");
    }
    return new ConsoleUserCredentials(
        organization.getConsoleUsername(),
        organization.getConsolePassword(),
        organization.getConsoleAccountId());
  }

  public ConsoleUserCredentials updateConsoleUserPassword(String orgId) {
    var org = organizationService.getById(orgId);
    var consoleUser = getConsoleUser(orgId);
    var updatedConsoleUserCredentials = iamComponent.updateUserPassword(consoleUser.username());
    organizationService.updateConsoleCredentials(
        orgId, updatedConsoleUserCredentials, org.getConsoleUserPolicyDocumentName());
    return updatedConsoleUserCredentials;
  }
}
