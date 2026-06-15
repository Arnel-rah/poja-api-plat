package api.poja.io.endpoint.rest.security;

import static api.poja.io.endpoint.rest.security.model.UserRole.ADMIN;
import static api.poja.io.repository.model.enums.OrganizationInviteStatus.*;
import static java.util.Arrays.asList;

import api.poja.io.model.User;
import api.poja.io.repository.model.AppInstallation;
import api.poja.io.repository.model.Application;
import api.poja.io.repository.model.Organization;
import api.poja.io.repository.model.OrganizationInvite;
import api.poja.io.service.AppInstallationService;
import api.poja.io.service.ApplicationService;
import api.poja.io.service.UserService;
import api.poja.io.service.organization.OrganizationService;
import api.poja.io.service.organization.OrganizationUsersService;
import java.util.Optional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class AuthenticatedResourceProvider {
  private final ApplicationService applicationService;
  private final UserService userService;
  private final AppInstallationService appInstallationService;
  private final OrganizationUsersService organizationUsersService;
  private final OrganizationService organizationService;

  public User getUser() {
    return AuthProvider.getPrincipal().getUser();
  }

  public Application getAuthenticatedApplication() {
    return AuthProvider.getApplicationPrincipal().getApplication();
  }

  public boolean isApplicationOwner(String userId, String applicationId) {
    Optional<Application> application = applicationService.findById(applicationId);
    return application.isPresent() && userId.equals(application.get().getUserId());
  }

  public boolean isInstallationOwner(String userId, String installationId) {
    Optional<AppInstallation> appInstallation = appInstallationService.findById(installationId);
    return appInstallation.isPresent() && userId.equals(appInstallation.get().getUserId());
  }

  public boolean isOrganizationMember(String userId, String orgId) {
    Optional<OrganizationInvite> latestUserInvite =
        organizationUsersService.findLatestUserInvite(orgId, userId, ACCEPTED);
    return latestUserInvite.isPresent();
  }

  public boolean isOrganizationOwner(String userId, String orgId) {
    Optional<Organization> organization = organizationService.findById(orgId);
    return organization.isPresent() && userId.equals(organization.get().getOwnerId());
  }

  public boolean isOrgMemberAndOrgIsAppOwner(String userId, String orgId, String applicationId) {
    Optional<Application> application = applicationService.findById(applicationId);
    return application.isPresent()
        && isOrganizationMember(userId, orgId)
        && application.get().getOrgId().equals(orgId);
  }

  public boolean isOrgOwnerAndOrgIsAppOwner(String userId, String orgId, String applicationId) {
    Optional<Application> application = applicationService.findById(applicationId);
    return application.isPresent()
        && isOrganizationOwner(userId, orgId)
        && application.get().getOrgId().equals(orgId);
  }

  public boolean isOrgMemberAndOrgIsAppInstallationOwner(
      String userId, String orgId, String installationId) {
    Optional<AppInstallation> appInstallation = appInstallationService.findById(installationId);
    return appInstallation.isPresent()
        && isOrganizationMember(userId, orgId)
        && appInstallation.get().getOrgId().equals(orgId);
  }

  public boolean isAdminUser(String userId) {
    var userOpt = userService.findById(userId);
    return userOpt.map(u -> asList(u.getRoles()).contains(ADMIN)).orElse(false);
  }
}
