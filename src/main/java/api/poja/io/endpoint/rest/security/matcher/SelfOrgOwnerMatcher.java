package api.poja.io.endpoint.rest.security.matcher;

import api.poja.io.endpoint.rest.security.AuthenticatedResourceProvider;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

public class SelfOrgOwnerMatcher extends SelfMatcher {

  public SelfOrgOwnerMatcher(
      HttpMethod method, String antPattern, AuthenticatedResourceProvider authResourceProvider) {
    super(method, antPattern, authResourceProvider);
  }

  @Override
  public boolean matches(HttpServletRequest request) {
    AntPathRequestMatcher antMatcher = new AntPathRequestMatcher(antPattern, method.toString());
    if (!antMatcher.matches(request)) {
      return false;
    }
    var authenticatedUserId = authResourceProvider.getUser().getId();
    return authResourceProvider.isOrganizationOwner(authenticatedUserId, getId(request));
  }
}
