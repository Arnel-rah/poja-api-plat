package api.poja.io.endpoint.rest.security;

import static api.poja.io.endpoint.rest.security.model.ApplicationRole.GITHUB_APPLICATION;
import static api.poja.io.endpoint.rest.security.model.UserRole.ADMIN;
import static api.poja.io.endpoint.rest.security.model.UserRole.USER;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.OPTIONS;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;
import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;
import static org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher;

import api.poja.io.endpoint.rest.security.filter.BearerAuthFilter;
import api.poja.io.endpoint.rest.security.filter.BetaFeaturesFilter;
import api.poja.io.endpoint.rest.security.filter.CustomFilterChainExceptionHandler;
import api.poja.io.endpoint.rest.security.filter.SuspendedUserFilter;
import api.poja.io.endpoint.rest.security.matcher.AdminOrRequestMatcher;
import api.poja.io.endpoint.rest.security.matcher.SelfOrgApplicationMatcher;
import api.poja.io.endpoint.rest.security.matcher.SelfOrgInstallationMatcher;
import api.poja.io.endpoint.rest.security.matcher.SelfOrgMatcher;
import api.poja.io.endpoint.rest.security.matcher.SelfOrgOwnerApplicationMatcher;
import api.poja.io.endpoint.rest.security.matcher.SelfOrgOwnerMatcher;
import api.poja.io.endpoint.rest.security.matcher.SelfUserApplicationMatcher;
import api.poja.io.endpoint.rest.security.matcher.SelfUserInstallationMatcher;
import api.poja.io.endpoint.rest.security.matcher.SelfUserMatcher;
import api.poja.io.model.exception.ForbiddenException;
import api.poja.io.model.exception.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.function.BiFunction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.servlet.HandlerExceptionResolver;

@EnableWebSecurity
@Configuration
@Slf4j
@Profile("!dev")
public class SecurityConf {

  private static final String AUTHORIZATION_HEADER = "Authorization";
  private static final OrRequestMatcher REQUIRES_AUTHENTICATION_REQUEST_MATCHER =
      new OrRequestMatcher(
          antMatcher(GET, "/users"),
          antMatcher(GET, "/users/*"),
          antMatcher(GET, "/whoami"),
          antMatcher(GET, "/beta-ping"),
          antMatcher(GET, "/billings"),
          antMatcher(GET, "/application-templates"),
          antMatcher(GET, "/application-templates/*"),
          antMatcher(GET, "/application-templates/*/config"),
          antMatcher(POST, "/orgs/*/application-templates/*/clone"),
          antMatcher(GET, "/orgs/*/consoleUser"),
          antMatcher(PUT, "/orgs/*/consoleUser"),
          antMatcher(GET, "/users/*/subscriptions"),
          antMatcher(POST, "/users/*/subscriptions"),
          antMatcher(GET, "/users/*/subscriptions/*"),
          antMatcher(DELETE, "/users/*/subscriptions/*"),
          antMatcher(POST, "/users/*/subscriptions/*/payment"),
          antMatcher(DELETE, "/users/*"),
          antMatcher(PUT, "/users/*/statuses"),
          antMatcher(GET, "/users/*/statuses"),
          antMatcher(GET, "/users/*/applications"),
          antMatcher(GET, "/users/*/installations"),
          antMatcher(PUT, "/users/*/installations"),
          antMatcher(GET, "/users/*/installations/*/repositories"),
          antMatcher(GET, "/orgs/*/installations"),
          antMatcher(PUT, "/orgs/*/installations"),
          antMatcher(GET, "/orgs/*/installations/*/repositories"),
          antMatcher(GET, "/users/*/installations/*/search/repositories"),
          antMatcher(PUT, "/orgs/*/applications"),
          antMatcher(GET, "/orgs/*/applications"),
          antMatcher(POST, "/orgs/*/application"),
          antMatcher(GET, "/orgs/*/applications/*"),
          antMatcher(PUT, "/orgs/*/applications/*/statuses"),
          antMatcher(GET, "/orgs/*/applications/*/deployments"),
          antMatcher(GET, "/orgs/*/applications/*/deployments/*"),
          antMatcher(GET, "/orgs/*/applications/*/deployments/*/config"),
          antMatcher(GET, "/orgs/*/applications/*/deployments/*/states"),
          antMatcher(GET, "/orgs/*/applications/*/setup/states"),
          antMatcher(GET, "/poja-versions"),
          antMatcher(PUT, "/poja-versions/*/changelog"),
          antMatcher(GET, "/orgs/*/applications/*/environments"),
          antMatcher(PUT, "/orgs/*/applications/*/environments"),
          antMatcher(GET, "/orgs/*/applications/*/environments/*"),
          antMatcher(PUT, "/orgs/*/applications/*/environments/*/statuses"),
          antMatcher(POST, "/orgs/*/applications/*/environments/*/deployments"),
          antMatcher(PUT, "/orgs/*/applications/*/environments/*/configs/*"),
          antMatcher(GET, "/orgs/*/applications/*/environments/*/configs/*"),
          antMatcher(POST, "/orgs/*/applications/*/environments/*/configs/*/deploy"),
          antMatcher(GET, "/orgs/*/applications/*/monitoringResources"),
          antMatcher(GET, "/orgs/*/applications/*/environments/*/resources"),
          antMatcher(GET, "/orgs/*/applications/*/log-queries/*"),
          antMatcher(POST, "/orgs/*/applications/*/log-queries"),
          antMatcher(GET, "/orgs/*/applications/*/environments/*/functions/*/logs"),
          antMatcher(GET, "/orgs/*/applications/*/environments/*/stacks"),
          antMatcher(GET, "/orgs/*/applications/*/environments/*/stacks/*"),
          antMatcher(GET, "/orgs/*/applications/*/environments/*/stacks/*/events"),
          antMatcher(GET, "/orgs/*/invitees/suggestions"),
          antMatcher(GET, "/users/*/payment-methods"),
          antMatcher(GET, "/gh-repos/*/*/upload-build-uri"),
          antMatcher(POST, "/gh-repos/*/*/github-workflow-state"),
          antMatcher(GET, "/users/*/payment-details"),
          antMatcher(PUT, "/users/*/payment-details"),
          antMatcher(GET, "/users/*/payment-details/payment-methods"),
          antMatcher(PUT, "/users/*/payment-details/payment-methods"),
          antMatcher(PUT, "/users/*/payments/*"),
          antMatcher(GET, "/users/*/payments"),
          antMatcher(PUT, "/gh-repos/*/*/env-deploys"),
          antMatcher(GET, "/users/*/billing"),
          antMatcher(GET, "/orgs/*/applications/*/billing"),
          antMatcher(GET, "/orgs/*/applications/*/environments/*/billing"),
          antMatcher(GET, "/users/*/orgs"),
          antMatcher(GET, "/users/*/orgs/billings"),
          antMatcher(PUT, "/users/*/orgs"),
          antMatcher(GET, "/users/*/org-invites"),
          antMatcher(PUT, "/users/*/org-invites"),
          antMatcher(PUT, "/users/*/billing-discounts"),
          antMatcher(GET, "/users/*/billing-discounts"),
          antMatcher(GET, "/users/*/payment-setup-states"),
          antMatcher(GET, "/orgs/*"),
          antMatcher(GET, "/orgs/*/billing"),
          antMatcher(GET, "/orgs/*/users"),
          antMatcher(PUT, "/orgs/*/users"),
          antMatcher(GET, "/orgs/*/invites"),
          antMatcher(DELETE, "/orgs/*/invites/*"),
          antMatcher(GET, "/orgs/*/setup-states"),
          antMatcher(POST, "/orgs/*/imports"),
          antMatcher(GET, "/orgs/*/imports"),
          antMatcher(GET, "/orgs/*/imports/*"),
          antMatcher(GET, "/orgs/*/imports/*/states"),
          antMatcher(POST, "/orgs/*/imports/*/states"),
          antMatcher(GET, "/users/*/cost"),
          antMatcher(GET, "/total-cost"));
  private final AuthProvider authProvider;
  private final HandlerExceptionResolver exceptionResolver;
  private final AuthenticatedResourceProvider authenticatedResourceProvider;
  private final boolean isPrivateBetaTest;
  private final CustomFilterChainExceptionHandler customFilterChainExceptionHandler;

  public SecurityConf(
      AuthProvider authProvider,
      // InternalToExternalErrorHandler behind
      @Qualifier("handlerExceptionResolver") HandlerExceptionResolver exceptionResolver,
      AuthenticatedResourceProvider authenticatedResourceProvider,
      @Value("${private.beta.test}") boolean isPrivateBetaTest,
      CustomFilterChainExceptionHandler customFilterChainExceptionHandler) {
    this.exceptionResolver = exceptionResolver;
    this.authProvider = authProvider;
    this.authenticatedResourceProvider = authenticatedResourceProvider;
    this.isPrivateBetaTest = isPrivateBetaTest;
    this.customFilterChainExceptionHandler = customFilterChainExceptionHandler;
  }

  private static Exception unauthorizedExceptionWithRemoteInfo(
      Exception e, HttpServletRequest req) {
    log.info(
        String.format(
            "Access is denied for remote caller: address=%s, host=%s, port=%s",
            req.getRemoteAddr(), req.getRemoteHost(), req.getRemotePort()));
    return new UnauthorizedException(e.getMessage());
  }

  private static Exception forbiddenWithRemoteInfo(Exception e, HttpServletRequest req) {
    log.info(
        String.format(
            "Access is denied for remote caller: address=%s, host=%s, port=%s",
            req.getRemoteAddr(), req.getRemoteHost(), req.getRemotePort()));
    return new ForbiddenException(e.getMessage());
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.exceptionHandling(
            (exceptionHandler) ->
                exceptionHandler
                    .authenticationEntryPoint(
                        // note(spring-exception)
                        // https://stackoverflow.com/questions/59417122/how-to-handle-usernamenotfoundexception-spring-security
                        // issues like when a user tries to access a resource
                        // without appropriate authentication elements
                        (req, res, e) ->
                            exceptionResolver.resolveException(
                                req, res, null, unauthorizedExceptionWithRemoteInfo(e, req)))
                    .accessDeniedHandler(
                        // note(spring-exception): issues like when a user not having required roles
                        (req, res, e) ->
                            exceptionResolver.resolveException(
                                req, res, null, forbiddenWithRemoteInfo(e, req))))
        .sessionManagement(session -> session.sessionCreationPolicy(STATELESS))
        .authenticationProvider(authProvider)
        .addFilterBefore(
            bearerFilter(REQUIRES_AUTHENTICATION_REQUEST_MATCHER),
            AnonymousAuthenticationFilter.class)
        .addFilterAfter(customFilterChainExceptionHandler, BearerAuthFilter.class)
        .addFilterAfter(
            betaActivationFilter(
                new OrRequestMatcher(
                    REQUIRES_AUTHENTICATION_REQUEST_MATCHER, antMatcher(GET, "/beta-ping"))),
            CustomFilterChainExceptionHandler.class)
        .addFilterAfter(
            suspendedUserFilter(
                new OrRequestMatcher(
                    antMatcher(GET, "/whoami"),
                    antMatcher(GET, "/token"),
                    antMatcher(POST, "/token"),
                    selfUserMatcher(GET, "/users/*"),
                    selfUserMatcher(GET, "/users/*/subscriptions"),
                    antMatcher(GET, "/user-stats"),
                    antMatcher(GET, "/users/*/statuses"),
                    antMatcher(GET, "/users/*/orgs"),
                    antMatcher(GET, "/users/*/orgs/billings"),
                    antMatcher(GET, "/users/*/billing-discounts"),
                    antMatcher(GET, "/users/*/billing"),
                    antMatcher(GET, "/orgs/*"),
                    antMatcher(GET, "/users/*/payment-details"),
                    antMatcher(PUT, "/users/*/payment-details"),
                    antMatcher(GET, "/users/*/payment-details/payment-methods"),
                    antMatcher(PUT, "/users/*/payment-details/payment-methods"),
                    antMatcher(GET, "/users/*/payments"),
                    antMatcher(PUT, "/users/*/payments/*")),
                REQUIRES_AUTHENTICATION_REQUEST_MATCHER),
            BetaFeaturesFilter.class)
        .authorizeHttpRequests(
            (authorize) ->
                authorize
                    .requestMatchers(OPTIONS, "/**")
                    .permitAll()
                    .requestMatchers(GET, "/ping")
                    .permitAll()
                    .requestMatchers(GET, "/beta-ping")
                    .authenticated()
                    .requestMatchers(GET, "/token")
                    .permitAll()
                    .requestMatchers(POST, "/token")
                    .permitAll()
                    .requestMatchers(GET, "/health/db")
                    .permitAll()
                    .requestMatchers(GET, "/health/bucket")
                    .permitAll()
                    .requestMatchers(POST, "/health/event/uuids")
                    .permitAll()
                    .requestMatchers(GET, "/health/event1")
                    .permitAll()
                    .requestMatchers(GET, "/health/event2")
                    .permitAll()
                    .requestMatchers(GET, "/health/email")
                    .permitAll()
                    .requestMatchers(GET, "/application-templates")
                    .authenticated()
                    .requestMatchers(GET, "/application-templates/*")
                    .authenticated()
                    .requestMatchers(GET, "/application-templates/*/config")
                    .authenticated()
                    .requestMatchers(
                        selfOrgOwnerMatcher(POST, "/orgs/*/application-templates/*/clone"))
                    .authenticated()
                    .requestMatchers(GET, "/users")
                    .authenticated()
                    .requestMatchers(POST, "/users")
                    .permitAll()
                    .requestMatchers(GET, "/user-stats")
                    .permitAll()
                    .requestMatchers("/offers")
                    .permitAll()
                    .requestMatchers("/offers/*")
                    .permitAll()
                    .requestMatchers(GET, "/users/*")
                    .authenticated()
                    .requestMatchers(DELETE, "/users/*")
                    .hasRole(ADMIN.getRole())
                    .requestMatchers(PUT, "/users/*/statuses")
                    .hasRole(ADMIN.getRole())
                    .requestMatchers(
                        adminOrRequestMatcher(GET, "/users/*/statuses", this::selfUserMatcher))
                    .authenticated()
                    .requestMatchers(selfUserMatcher(GET, "/users/*/applications"))
                    .authenticated()
                    .requestMatchers(GET, "/whoami")
                    .authenticated()
                    .requestMatchers(GET, "/billings")
                    .hasRole(ADMIN.getRole())
                    .requestMatchers(GET, "/poja-versions")
                    .authenticated()
                    .requestMatchers(PUT, "/poja-versions/*/changelog")
                    .hasRole(ADMIN.getRole())
                    .requestMatchers(GET, "/orgs/*")
                    .authenticated()
                    .requestMatchers(selfOrgMatcher(GET, "/orgs/*/users"))
                    .authenticated()
                    .requestMatchers(selfOrgOwnerMatcher(GET, "/orgs/*/billing"))
                    .authenticated()
                    .requestMatchers(selfOrgMatcher(PUT, "/orgs/*/users"))
                    .authenticated()
                    .requestMatchers(selfOrgMatcher(GET, "/orgs/*/consoleUser"))
                    .authenticated()
                    .requestMatchers(selfOrgMatcher(PUT, "/orgs/*/consoleUser"))
                    .authenticated()
                    .requestMatchers(selfUserMatcher(GET, "/users/*/subscriptions"))
                    .authenticated()
                    .requestMatchers(selfUserMatcher(POST, "/users/*/subscriptions"))
                    .authenticated()
                    .requestMatchers(GET, "/users/*/subscriptions/*")
                    .authenticated()
                    .requestMatchers(selfUserMatcher(DELETE, "/users/*/subscriptions/*"))
                    .authenticated()
                    .requestMatchers(PUT, "/users/*/billing-discounts")
                    .hasRole(ADMIN.getRole())
                    .requestMatchers(selfUserMatcher(GET, "/users/*/billing-discounts"))
                    .hasRole(USER.getRole())
                    .requestMatchers(GET, "/users/*/billing-discounts")
                    .hasRole(ADMIN.getRole())
                    .requestMatchers(selfUserMatcher(POST, "/users/*/subscriptions/*/payment"))
                    .authenticated()
                    .requestMatchers(GET, "/users/*/payment-details")
                    .authenticated()
                    .requestMatchers(PUT, "/users/*/payment-details")
                    .authenticated()
                    .requestMatchers(GET, "/users/*/payment-details/payment-methods")
                    .authenticated()
                    .requestMatchers(PUT, "/users/*/payment-details/payment-methods")
                    .authenticated()
                    .requestMatchers(PUT, "/users/*/payments/*")
                    .authenticated()
                    .requestMatchers(GET, "/users/*/payments")
                    .authenticated()
                    .requestMatchers(selfUserMatcher(GET, "/users/*/installations"))
                    .authenticated()
                    .requestMatchers(selfUserMatcher(PUT, "/users/*/installations"))
                    .authenticated()
                    .requestMatchers(
                        selfUserInstallationMatcher(GET, "/users/*/installations/*/repositories"))
                    .authenticated()
                    .requestMatchers(
                        selfUserInstallationMatcher(
                            GET, "/users/*/installations/*/search/repositories"))
                    .authenticated()
                    .requestMatchers(selfUserMatcher(GET, "/users/*/payment-setup-states"))
                    .authenticated()
                    .requestMatchers(GET, "/users/*/payment-setup-states")
                    .hasRole(ADMIN.getRole())
                    .requestMatchers(selfOrgMatcher(PUT, "/orgs/*/installations"))
                    .authenticated()
                    .requestMatchers(selfOrgMatcher(GET, "/orgs/*/installations"))
                    .authenticated()
                    .requestMatchers(
                        selfOrgInstallationMatcher(GET, "/orgs/*/installations/*/repositories"))
                    .authenticated()
                    .requestMatchers(selfOrgOwnerMatcher(PUT, "/orgs/*/applications"))
                    .authenticated()
                    .requestMatchers(selfOrgOwnerMatcher(POST, "/orgs/*/application"))
                    .authenticated()
                    .requestMatchers(selfOrgMatcher(GET, "/orgs/*/applications"))
                    .authenticated()
                    .requestMatchers(selfOrgMatcher(GET, "/orgs/*/applications/*"))
                    .authenticated()
                    .requestMatchers(
                        selfOrgOwnerApplicationMatcher(PUT, "/orgs/*/applications/*/statuses"))
                    .authenticated()
                    .requestMatchers(
                        selfOrgApplicationMatcher(GET, "/orgs/*/applications/*/deployments"))
                    .authenticated()
                    .requestMatchers(
                        selfOrgApplicationMatcher(GET, "/orgs/*/applications/*/deployments/*"))
                    .authenticated()
                    .requestMatchers(
                        selfOrgApplicationMatcher(
                            GET, "/orgs/*/applications/*/deployments/*/config"))
                    .authenticated()
                    .requestMatchers(
                        selfOrgApplicationMatcher(
                            GET, "/orgs/*/applications/*/deployments/*/states"))
                    .authenticated()
                    .requestMatchers(
                        selfOrgApplicationMatcher(GET, "/orgs/*/applications/*/setup/states"))
                    .authenticated()
                    .requestMatchers(
                        selfOrgApplicationMatcher(GET, "/orgs/*/applications/*/environments"))
                    .authenticated()
                    .requestMatchers(
                        selfOrgOwnerMatcher(PUT, "/orgs/*/applications/*/environments"))
                    .authenticated()
                    .requestMatchers(
                        selfOrgApplicationMatcher(GET, "/orgs/*/applications/*/environments/*"))
                    .authenticated()
                    .requestMatchers(
                        selfOrgOwnerApplicationMatcher(
                            PUT, "/orgs/*/applications/*/environments/*/statuses"))
                    .authenticated()
                    .requestMatchers(
                        selfOrgApplicationMatcher(
                            POST, "/orgs/*/applications/*/environments/*/deployments"))
                    .authenticated()
                    .requestMatchers(
                        selfOrgApplicationMatcher(
                            PUT, "/orgs/*/applications/*/environments/*/configs/*"))
                    .authenticated()
                    .requestMatchers(
                        selfOrgApplicationMatcher(
                            GET, "/orgs/*/applications/*/environments/*/configs/*"))
                    .authenticated()
                    .requestMatchers(
                        selfOrgApplicationMatcher(
                            POST, "/orgs/*/applications/*/environments/*/configs/*/deploy"))
                    .authenticated()
                    .requestMatchers(
                        selfOrgApplicationMatcher(
                            GET, "/orgs/*/applications/*/environments/*/stacks"))
                    .authenticated()
                    .requestMatchers(
                        selfOrgApplicationMatcher(
                            GET, "/orgs/*/applications/*/environments/*/stacks/*"))
                    .authenticated()
                    .requestMatchers(
                        selfOrgApplicationMatcher(
                            GET, "/orgs/*/applications/*/environments/*/stacks/*/events"))
                    .authenticated()
                    .requestMatchers(
                        selfOrgApplicationMatcher(
                            GET, "/orgs/*/applications/*/monitoringResources"))
                    .authenticated()
                    .requestMatchers(
                        selfOrgApplicationMatcher(
                            GET, "/orgs/*/applications/*/environments/*/resources"))
                    .authenticated()
                    .requestMatchers(
                        selfOrgApplicationMatcher(
                            GET, "/orgs/*/applications/*/environments/*/functions/*/logs"))
                    .authenticated()
                    .requestMatchers(
                        selfOrgApplicationMatcher(GET, "/orgs/*/applications/*/log-queries/*"))
                    .authenticated()
                    .requestMatchers(
                        selfOrgApplicationMatcher(POST, "/orgs/*/applications/*/log-queries"))
                    .authenticated()
                    .requestMatchers(selfOrgMatcher(GET, "/orgs/*/invitees/suggestions"))
                    .authenticated()
                    .requestMatchers(selfOrgOwnerMatcher(GET, "/orgs/*/invites"))
                    .authenticated()
                    .requestMatchers(selfOrgOwnerMatcher(DELETE, "/orgs/*/invites/*"))
                    .authenticated()
                    .requestMatchers(GET, "/orgs/*/setup-states")
                    .authenticated()
                    .requestMatchers(GET, "/gh-repos/*/*/upload-build-uri")
                    .hasRole(GITHUB_APPLICATION.getRole())
                    .requestMatchers(POST, "/gh-repos/*/*/github-workflow-state")
                    .hasRole(GITHUB_APPLICATION.getRole())
                    .requestMatchers(PUT, "/gh-repos/*/*/env-deploys")
                    .hasRole(GITHUB_APPLICATION.getRole())
                    .requestMatchers(
                        selfOrgApplicationMatcher(GET, "/orgs/*/applications/*/billing"))
                    .authenticated()
                    .requestMatchers(
                        selfOrgApplicationMatcher(
                            GET, "/orgs/*/applications/*/environments/*/billing"))
                    .authenticated()
                    .requestMatchers(selfUserMatcher(GET, "/users/*/billing"))
                    .authenticated()
                    .requestMatchers(selfUserMatcher(GET, "/users/*/orgs"))
                    .authenticated()
                    .requestMatchers(selfUserMatcher(GET, "/users/*/orgs/billings"))
                    .authenticated()
                    .requestMatchers(selfUserMatcher(PUT, "/users/*/orgs"))
                    .authenticated()
                    .requestMatchers(selfUserMatcher(GET, "/users/*/org-invites"))
                    .authenticated()
                    .requestMatchers(selfUserMatcher(PUT, "/users/*/org-invites"))
                    .authenticated()
                    .requestMatchers(selfOrgOwnerMatcher(POST, "/orgs/*/imports"))
                    .authenticated()
                    .requestMatchers(POST, "/orgs/*/imports/*/states")
                    .hasRole(GITHUB_APPLICATION.getRole())
                    .requestMatchers(selfOrgMatcher(GET, "/orgs/*/imports"))
                    .authenticated()
                    .requestMatchers(selfOrgMatcher(GET, "/orgs/*/imports/*"))
                    .authenticated()
                    .requestMatchers(selfOrgMatcher(GET, "/orgs/*/imports/*/states"))
                    .authenticated()
                    .requestMatchers(GET, "/users/*/cost")
                    .hasRole(ADMIN.getRole())
                    .requestMatchers(GET, "/total-cost")
                    .hasRole(ADMIN.getRole())
                    .requestMatchers("/**")
                    .denyAll())
        // disable superfluous protections
        // Eg if all clients are non-browser then no csrf
        // https://docs.spring.io/spring-security/site/docs/3.2.0.CI-SNAPSHOT/reference/html/csrf.html,
        // Sec 13.3
        .csrf(AbstractHttpConfigurer::disable)
        .formLogin(AbstractHttpConfigurer::disable)
        .logout(AbstractHttpConfigurer::disable);
    // formatter:on
    return http.build();
  }

  private RequestMatcher selfUserMatcher(HttpMethod method, String antPath) {
    return new SelfUserMatcher(method, antPath, authenticatedResourceProvider);
  }

  private RequestMatcher selfUserApplicationMatcher(HttpMethod method, String antPath) {
    return new SelfUserApplicationMatcher(method, antPath, authenticatedResourceProvider);
  }

  private RequestMatcher selfUserInstallationMatcher(HttpMethod method, String antPath) {
    return new SelfUserInstallationMatcher(method, antPath, authenticatedResourceProvider);
  }

  /** matches org resource id with its owner or any of its members */
  private RequestMatcher selfOrgMatcher(HttpMethod method, String antPath) {
    return new SelfOrgMatcher(method, antPath, authenticatedResourceProvider);
  }

  private RequestMatcher selfOrgOwnerMatcher(HttpMethod method, String antPath) {
    return new SelfOrgOwnerMatcher(method, antPath, authenticatedResourceProvider);
  }

  private RequestMatcher selfOrgOwnerApplicationMatcher(HttpMethod method, String antPath) {
    return new SelfOrgOwnerApplicationMatcher(method, antPath, authenticatedResourceProvider);
  }

  private RequestMatcher selfOrgApplicationMatcher(HttpMethod method, String antPath) {
    return new SelfOrgApplicationMatcher(method, antPath, authenticatedResourceProvider);
  }

  private RequestMatcher selfOrgInstallationMatcher(HttpMethod method, String antPath) {
    return new SelfOrgInstallationMatcher(method, antPath, authenticatedResourceProvider);
  }

  @SafeVarargs
  private RequestMatcher adminOrRequestMatcher(
      HttpMethod method,
      String pattern,
      BiFunction<HttpMethod, String, RequestMatcher>... requestMatchers) {
    return new AdminOrRequestMatcher(
        antMatcher(method, pattern),
        authenticatedResourceProvider,
        new OrRequestMatcher(
            Arrays.stream(requestMatchers).map(m -> m.apply(method, pattern)).toList()));
  }

  @Bean
  public AuthenticationManager authenticationManager() {
    return new ProviderManager(authProvider);
  }

  private BearerAuthFilter bearerFilter(RequestMatcher requiresAuthenticationRequestMatcher) {
    BearerAuthFilter bearerFilter =
        new BearerAuthFilter(requiresAuthenticationRequestMatcher, AUTHORIZATION_HEADER);
    bearerFilter.setAuthenticationManager(authenticationManager());
    bearerFilter.setAuthenticationSuccessHandler(
        (httpServletRequest, httpServletResponse, authentication) -> {});
    bearerFilter.setAuthenticationFailureHandler(
        (req, res, e) ->
            exceptionResolver.resolveException(
                req, res, null, unauthorizedExceptionWithRemoteInfo(e, req)));

    return bearerFilter;
  }

  private BetaFeaturesFilter betaActivationFilter(RequestMatcher requestMatcher) {
    return new BetaFeaturesFilter(requestMatcher, isPrivateBetaTest);
  }

  private SuspendedUserFilter suspendedUserFilter(
      RequestMatcher accessibleBySuspendedUserRequestMatcher,
      RequestMatcher requiresAuthenticationRequestMatcher) {
    return new SuspendedUserFilter(
        accessibleBySuspendedUserRequestMatcher, requiresAuthenticationRequestMatcher);
  }
}
