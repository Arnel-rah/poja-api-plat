package api.poja.io.endpoint.rest.security;

import static api.poja.io.endpoint.rest.security.GithubUserBearerAuthenticator.BEARER_PREFIX;
import static java.util.Optional.empty;

import api.poja.io.endpoint.rest.security.github.GithubComponent;
import api.poja.io.endpoint.rest.security.model.ApplicationPrincipal;
import api.poja.io.model.User;
import api.poja.io.repository.model.Application;
import api.poja.io.repository.model.Organization;
import api.poja.io.service.ApplicationService;
import api.poja.io.service.UserService;
import api.poja.io.service.organization.OrganizationService;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

@AllArgsConstructor
@Component
@Slf4j
public class GithubAppBearerAuthenticator implements UsernamePasswordAuthenticator {
  public static final String APP_BEARER_PREFIX = "AppBearer ";
  private final GithubComponent githubComponent;
  private final ApplicationService applicationService;
  private final UserService userService;
  private final OrganizationService organizationService;

  @Override
  public UserDetails retrieveUser(
      String username, UsernamePasswordAuthenticationToken authentication) {
    Optional<String> optBearer = getBearerFromHeader(authentication);
    if (optBearer.isEmpty()) {
      throw new BadCredentialsException("Bad credentials"); // NOSONAR
    }
    String bearer = optBearer.get();
    Optional<String> repositoryId = githubComponent.getRepositoryIdByAppToken(bearer);
    if (repositoryId.isEmpty()) {
      throw new BadCredentialsException("Bad credentials");
    }
    String repoId = repositoryId.get();
    try {
      Application application = applicationService.getByRepositoryIdAndArchived(repoId, false);
      Organization org = organizationService.getById(application.getOrgId());
      User user = userService.getUserById(org.getOwnerId());
      return new ApplicationPrincipal(application, org, user, bearer);
    } catch (Exception e) {
      throw new UsernameNotFoundException("app not found");
    }
  }

  private static Optional<String> getBearerFromHeader(
      UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken) {
    Object tokenObject = usernamePasswordAuthenticationToken.getCredentials();
    if (!(tokenObject instanceof String token)
        || (!token.startsWith(BEARER_PREFIX) && !token.startsWith(APP_BEARER_PREFIX))) {
      return empty();
    }
    if (token.startsWith(BEARER_PREFIX)) {
      log.info("deprecated, use AppBearer as a prefix");
      return Optional.of(token.substring(BEARER_PREFIX.length()).trim());
    }
    return Optional.of(token.substring(APP_BEARER_PREFIX.length()).trim());
  }
}
