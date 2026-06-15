package api.poja.io.endpoint.rest.security;

import static java.util.Optional.empty;

import api.poja.io.endpoint.rest.security.github.GithubComponent;
import api.poja.io.endpoint.rest.security.model.Principal;
import api.poja.io.model.User;
import api.poja.io.model.exception.NotFoundException;
import api.poja.io.service.UserService;
import java.util.Optional;
import lombok.AllArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

@AllArgsConstructor
@Component
public class GithubUserBearerAuthenticator implements UsernamePasswordAuthenticator {
  public static final String BEARER_PREFIX = "Bearer ";

  private final GithubComponent githubComponent;
  private final UserService userService;

  @Override
  public UserDetails retrieveUser(
      String username, UsernamePasswordAuthenticationToken authentication) {
    Optional<String> optBearer = getBearerFromHeader(authentication);
    if (optBearer.isEmpty()) {
      throw new BadCredentialsException("Bad credentials"); // NOSONAR
    }
    String bearer = optBearer.get();
    Optional<String> githubUserId = githubComponent.getGithubUserId(bearer);
    if (githubUserId.isEmpty()) {
      throw new BadCredentialsException("Bad credentials"); // NOSONAR
    }
    try {
      User user = userService.findByGithubUserId(githubUserId.get());
      return new Principal(user, bearer);
    } catch (NotFoundException e) {
      throw new UsernameNotFoundException("username not found");
    }
  }

  private static Optional<String> getBearerFromHeader(
      UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken) {
    Object tokenObject = usernamePasswordAuthenticationToken.getCredentials();
    if (!(tokenObject instanceof String token) || !token.startsWith(BEARER_PREFIX)) {
      return empty();
    }
    return Optional.of(token.substring(BEARER_PREFIX.length()).trim());
  }
}
