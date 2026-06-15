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
public class SelfUserInstallationMatcher implements RequestMatcher {
  private static final Pattern SELFABLE_URI_PATTERN =
      // /users/id/installations/id/...
      Pattern.compile("/[^/]+/(?<userId>[^/]+)/[^/]+/(?<installationId>[^/]+)(/.*)?");
  protected final HttpMethod method;
  protected final String antPattern;
  protected final AuthenticatedResourceProvider authResourceProvider;

  @Override
  public boolean matches(HttpServletRequest request) {
    AntPathRequestMatcher antMatcher = new AntPathRequestMatcher(antPattern, method.toString());
    if (!antMatcher.matches(request)) {
      return false;
    }
    var selfUserId = getSelfUserId(getUriMatcher(request));
    var selfInstallationId = getSelfInstallationId(getUriMatcher(request));
    var authenticatedUserId = AuthProvider.getPrincipal().getUser().getId();
    assert selfUserId != null;
    if (selfUserId.equals(authenticatedUserId)) {
      return authResourceProvider.isInstallationOwner(selfUserId, selfInstallationId);
    }
    return false;
  }

  private String getSelfUserId(Matcher matcher) {
    return matcher.find() ? matcher.group("userId") : null;
  }

  private static Matcher getUriMatcher(HttpServletRequest request) {
    return SELFABLE_URI_PATTERN.matcher(request.getRequestURI());
  }

  private String getSelfInstallationId(Matcher matcher) {
    return matcher.find() ? matcher.group("installationId") : null;
  }
}
