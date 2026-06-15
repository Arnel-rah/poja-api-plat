package api.poja.io.endpoint.rest.security;

import static api.poja.io.endpoint.rest.security.GithubAppBearerAuthenticator.APP_BEARER_PREFIX;
import static api.poja.io.endpoint.rest.security.GithubAppImportBearerAuthenticator.APP_IMPORT_BEARER_PREFIX;
import static java.util.Optional.empty;

import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

@Primary
@Component
@AllArgsConstructor
@Slf4j
public class UsernamePasswordAuthenticatorFacade implements UsernamePasswordAuthenticator {
  private final GithubAppBearerAuthenticator githubAppBearerAuthenticator;
  private final GithubUserBearerAuthenticator githubUserBearerAuthenticator;
  private final GithubAppImportBearerAuthenticator githubAppImportBearerAuthenticator;

  @Override
  public UserDetails retrieveUser(
      String username, UsernamePasswordAuthenticationToken authentication) {
    Optional<String> optionalBearer = getBearerFromHeader(authentication);
    if (optionalBearer.isEmpty()) {
      throw new BadCredentialsException("Bad credentials");
    }
    String bearer = optionalBearer.get();
    if (bearer.startsWith(APP_BEARER_PREFIX)) {
      return githubAppBearerAuthenticator.retrieveUser(username, authentication);
    }
    if (bearer.startsWith(APP_IMPORT_BEARER_PREFIX)) {
      return githubAppImportBearerAuthenticator.retrieveUser(username, authentication);
    }
    try {
      return githubUserBearerAuthenticator.retrieveUser(username, authentication);
    } catch (UsernameNotFoundException e) {
      throw e;
    } catch (Exception e) {
      log.info("error during auth as user", e);
      return githubAppBearerAuthenticator.retrieveUser(username, authentication);
    }
  }

  private static Optional<String> getBearerFromHeader(
      UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken) {
    Object tokenObject = usernamePasswordAuthenticationToken.getCredentials();
    if (!(tokenObject instanceof String token)) {
      return empty();
    }
    return Optional.of(token);
  }
}
