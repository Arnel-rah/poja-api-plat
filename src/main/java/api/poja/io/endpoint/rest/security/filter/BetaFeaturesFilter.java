package api.poja.io.endpoint.rest.security.filter;

import api.poja.io.endpoint.rest.security.AuthProvider;
import api.poja.io.endpoint.rest.security.model.Principal;
import api.poja.io.model.exception.ForbiddenException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.AllArgsConstructor;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

@AllArgsConstructor
public class BetaFeaturesFilter extends OncePerRequestFilter {
  private final RequestMatcher requiresIsBetaTestUserRequestMatchers;
  private final boolean isPrivateBetaTest;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    var authentication = AuthProvider.getAuthentication().getPrincipal();
    if (authentication instanceof Principal principal) {
      if (!principal.isBetaTester()) {
        throw new ForbiddenException("Access Denied");
      }
    }

    filterChain.doFilter(request, response);
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return !isPrivateBetaTest && requiresIsBetaTestUserRequestMatchers.matches(request);
  }
}
