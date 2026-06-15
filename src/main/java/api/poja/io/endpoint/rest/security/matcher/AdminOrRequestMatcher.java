package api.poja.io.endpoint.rest.security.matcher;

import api.poja.io.endpoint.rest.security.AuthProvider;
import api.poja.io.endpoint.rest.security.AuthenticatedResourceProvider;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

@AllArgsConstructor
@Slf4j
public final class AdminOrRequestMatcher implements RequestMatcher {
  private final AuthenticatedResourceProvider authResourceProvider;
  private final OrRequestMatcher orRequestMatcher;
  private final AntPathRequestMatcher antMatcher;

  public AdminOrRequestMatcher(
      AntPathRequestMatcher antMatcher,
      AuthenticatedResourceProvider authResourceProvider,
      OrRequestMatcher orRequestMatcher) {
    this.orRequestMatcher = orRequestMatcher;
    this.authResourceProvider = authResourceProvider;
    this.antMatcher = antMatcher;
  }

  @Override
  public boolean matches(HttpServletRequest request) {
    if (!antMatcher.matches(request)) {
      return false;
    }
    var authenticatedUserId = AuthProvider.getPrincipal().getUser().getId();
    if (authResourceProvider.isAdminUser(authenticatedUserId)) {
      return true;
    }
    return orRequestMatcher.matches(request);
  }
}
