package api.poja.io.endpoint.rest.security.filter;

import api.poja.io.model.exception.ForbiddenException;
import api.poja.io.model.exception.PaymentRequiredException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

@Component
public class CustomFilterChainExceptionHandler extends OncePerRequestFilter {
  private final HandlerExceptionResolver exceptionResolver;

  public CustomFilterChainExceptionHandler(
      @Qualifier("handlerExceptionResolver") HandlerExceptionResolver exceptionResolver) {
    this.exceptionResolver = exceptionResolver;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    try {
      filterChain.doFilter(request, response);
    } catch (ForbiddenException | PaymentRequiredException e) {
      logger.error(e.getMessage(), e);
      exceptionResolver.resolveException(request, response, null, e);
    }
  }
}
