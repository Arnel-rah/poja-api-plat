package api.poja.io.endpoint.rest.security.matcher;

import api.poja.io.endpoint.rest.security.AuthenticatedResourceProvider;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

/** matches org resource id with its owner or any of its members */
public class SelfOrgMatcher extends SelfMatcher {
  public SelfOrgMatcher(
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
    return authResourceProvider.isOrganizationMember(authenticatedUserId, getId(request));
  }
}
