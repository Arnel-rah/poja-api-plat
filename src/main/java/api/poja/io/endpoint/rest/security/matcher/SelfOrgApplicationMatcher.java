package api.poja.io.endpoint.rest.security.matcher;

import api.poja.io.endpoint.rest.security.AuthProvider;
import api.poja.io.endpoint.rest.security.AuthenticatedResourceProvider;
import jakarta.servlet.http.HttpServletRequest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

@AllArgsConstructor
@Slf4j
public class SelfOrgApplicationMatcher implements RequestMatcher {
  private static final Pattern SELFABLE_URI_PATTERN =
      // /orgs/id/applications/id/...
      Pattern.compile("/[^/]+/(?<orgId>[^/]+)/[^/]+/(?<applicationId>[^/]+)(/.*)?");
  protected final HttpMethod method;
  protected final String antPattern;
  protected final AuthenticatedResourceProvider authResourceProvider;

  @Override
  public boolean matches(HttpServletRequest request) {
    AntPathRequestMatcher antMatcher = new AntPathRequestMatcher(antPattern, method.toString());
    if (!antMatcher.matches(request)) {
      return false;
    }
    var selfOrgId = getSelfOrgId(getUriMatcher(request));
    var selfApplicationId = getSelfApplicationId(getUriMatcher(request));
    var authenticatedUserId = AuthProvider.getPrincipal().getUser().getId();
    assert selfOrgId != null;
    return authResourceProvider.isOrgMemberAndOrgIsAppOwner(
        authenticatedUserId, selfOrgId, selfApplicationId);
  }

  private String getSelfOrgId(Matcher matcher) {
    return matcher.find() ? matcher.group("orgId") : null;
  }

  private static Matcher getUriMatcher(HttpServletRequest request) {
    return SELFABLE_URI_PATTERN.matcher(request.getRequestURI());
  }

  private String getSelfApplicationId(Matcher matcher) {
    return matcher.find() ? matcher.group("applicationId") : null;
  }
}
