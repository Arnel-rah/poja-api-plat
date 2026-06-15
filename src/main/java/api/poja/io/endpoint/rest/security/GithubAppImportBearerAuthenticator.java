package api.poja.io.endpoint.rest.security;

import static api.poja.io.endpoint.rest.security.GithubUserBearerAuthenticator.BEARER_PREFIX;
import static java.util.Optional.empty;

import api.poja.io.endpoint.rest.security.github.GithubComponent;
import api.poja.io.endpoint.rest.security.model.ApplicationImportPrincipal;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@AllArgsConstructor
@Component
@Slf4j
public class GithubAppImportBearerAuthenticator implements UsernamePasswordAuthenticator {
  public static final String APP_IMPORT_BEARER_PREFIX = "AppImportBearer ";
  private final GithubComponent githubComponent;

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
    return new ApplicationImportPrincipal(repoId, bearer);
  }

  private static Optional<String> getBearerFromHeader(
      UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken) {
    Object tokenObject = usernamePasswordAuthenticationToken.getCredentials();
    if (!(tokenObject instanceof String token)
        || (!token.startsWith(BEARER_PREFIX) && !token.startsWith(APP_IMPORT_BEARER_PREFIX))) {
      return empty();
    }
    if (token.startsWith(BEARER_PREFIX)) {
      log.info("deprecated, use AppImportBearer as a prefix");
      return Optional.of(token.substring(BEARER_PREFIX.length()).trim());
    }
    return Optional.of(token.substring(APP_IMPORT_BEARER_PREFIX.length()).trim());
  }
}
