package api.poja.io.endpoint.rest.security.filter;

import static api.poja.io.model.UserStatus.SUSPENDED;
import static api.poja.io.service.event.RefreshUserStatusRequestedService.FREE_TIER_EXCEEDED_STATUS_REASON;
import static api.poja.io.service.event.RefreshUserStatusRequestedService.HAS_UNPAID_SUB_AND_DOES_NOT_COMPLY_TO_BASIC_OFFER;
import static api.poja.io.service.event.RefreshUserStatusRequestedService.PAYMENT_OVERDUE_STATUS_REASON;

import api.poja.io.endpoint.rest.security.AuthProvider;
import api.poja.io.endpoint.rest.security.model.ApplicationPrincipal;
import api.poja.io.endpoint.rest.security.model.Principal;
import api.poja.io.model.exception.PaymentRequiredException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

@AllArgsConstructor
@Slf4j
public class SuspendedUserFilter extends OncePerRequestFilter {
  private final RequestMatcher accessibleBySuspendedUserRequestMatcher;
  private final RequestMatcher requiresAuthenticationRequestMatcher;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    var principal = AuthProvider.getAuthentication().getPrincipal();
    if (principal instanceof Principal p) {
      if (hasPaymentIssues(p)) {
        throw new PaymentRequiredException();
      }
    }
    if (principal instanceof ApplicationPrincipal p) {
      if (p.isSuspended()) {
        throw new PaymentRequiredException();
      }
    }

    filterChain.doFilter(request, response);
  }

  private static boolean hasPaymentIssues(Principal principal) {
    if (!SUSPENDED.equals(principal.getStatus())) {
      return false;
    }
    var statusReason = principal.getStatusReason();
    return PAYMENT_OVERDUE_STATUS_REASON.equals(statusReason)
        || FREE_TIER_EXCEEDED_STATUS_REASON.equals(statusReason)
        || HAS_UNPAID_SUB_AND_DOES_NOT_COMPLY_TO_BASIC_OFFER.equals(statusReason);
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    var accessible = accessibleBySuspendedUserRequestMatcher.matches(request);
    if (requiresAuthenticationRequestMatcher.matches(request)) {
      return accessible;
    }
    return accessible;
  }
}
