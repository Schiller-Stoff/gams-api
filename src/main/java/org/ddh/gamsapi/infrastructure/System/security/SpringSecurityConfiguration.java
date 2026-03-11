package org.ddh.gamsapi.infrastructure.System.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Spring security configuration.
 * <p>
 * Domain API endpoints live under {@code /api/v1/} (versioned).
 * Authentication infrastructure endpoints live under {@code /api/auth/} (unversioned),
 * keeping auth concerns separate from the versioned API contract.
 * <p>
 * Both prefixes share the common {@code /api/} root, allowing reverse proxies
 * to route all application traffic via a single {@code /api/**} rule.
 * <p>
 * Auth endpoint overview:
 * <ul>
 *   <li>{@code GET  /api/auth/login} — login entry point (redirects to OAuth2 provider)</li>
 *   <li>{@code GET  /api/auth/oauth2/authorization/{registrationId}} — OAuth2 authorization redirect</li>
 *   <li>{@code GET  /api/auth/oauth2/callback/{registrationId}} — OAuth2 callback from provider</li>
 *   <li>{@code POST /api/auth/logout} — logout processing (CSRF-protected)</li>
 * </ul>
 */
@Configuration
@Slf4j
@RequiredArgsConstructor
public class SpringSecurityConfiguration {

  private final UserProjectAuthorizationManager userProjectAuthorizationManager;

  private final ClientRegistrationRepository clientRegistrationRepository;

  /**
   * Combined spring security matchers.
   * Matches all endpoints that require an admin authorization
   * (e.g. used along restrictions to DELETE / POST requests.)
   */
  private final String[] ADMIN_ONLY_PATHS = {"/api/v1/user**", "/api/v1/projects/{projectAbbr}"};

  private final String[] PUBLIC_GET_PATHS = {"/api/v1**", "/api/v1/**"};


  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

    log.info("*** Initializing spring security config ***");


    // configure oauth2 login — auth infrastructure under /api/auth/ (separate from versioned API)
    // loginPage: where unauthenticated users are redirected (handled by AuthEndpointController)
    // authorizationEndpoint.baseUri: remaps /oauth2/authorization/* to /api/auth/oauth2/authorization/*
    // redirectionEndpoint.baseUri: remaps /login/oauth2/code/* to /api/auth/oauth2/callback/*
    // failureUrl: where to redirect after a failed OAuth2 login (handled by AuthEndpointController)
    http.oauth2Login(oauth2 -> {
      oauth2
          .loginPage("/api/auth/login")
          .authorizationEndpoint(auth -> auth.baseUri("/api/auth/oauth2/authorization"))
          .redirectionEndpoint(redirect -> redirect.baseUri("/api/auth/oauth2/callback/*"))
          .defaultSuccessUrl("/api/v1", true)
          .failureUrl("/api/auth/login?error=true");
    });

    // handling logout — processing endpoint at /api/auth/logout
    http.logout(logout -> logout
        .logoutUrl("/api/auth/logout")
        .logoutSuccessHandler(oidcLogoutSuccessHandler())
        .invalidateHttpSession(true)
        .clearAuthentication(true)
        .deleteCookies("JSESSIONID")
    );

    http.authorizeHttpRequests(auth ->
        auth
            // allow post requests against specific integration api endpoints (because: might get queries via POST)
            // TODO think about stricter security check (must be query for solr / sparql / deny if to big content etc.)
            .requestMatchers(HttpMethod.POST,"/api/v1/integration/rdf*","/api/v1/integration/search*")
            .permitAll()
            // the datastream content auth is handled at controller level!
            //.permitAll()
            // All HEAD and GET after above rules are allowed
            .requestMatchers(request -> {
              String requestMethod = request.getMethod();
              return switch (requestMethod) {
                case "GET", "HEAD", "OPTIONS" -> true;
                default -> false;
              };
            })
            .permitAll()
            // authorization only applies for these endpoints
            .requestMatchers("/api/v1/projects/{projectAbbr}/objects/**", "/api/v1/integration/projects/{projectAbbr}/objects/**")
            .access(userProjectAuthorizationManager)
            // projects may only be created / deleted by global admin role
            .requestMatchers(HttpMethod.PUT,"/api/v1/projects/{projectAbbr}/", "/api/v1/projects/{projectAbbr}")
            .hasAuthority(GAMSAPIAuthorities.getSuperAdmin())
            .requestMatchers(HttpMethod.DELETE,"/api/v1/projects/{projectAbbr}/", "/api/v1/projects/{projectAbbr}")
            .hasAuthority(GAMSAPIAuthorities.getSuperAdmin())
            // any not matched requests require authentication
            .anyRequest()
            .authenticated()

    );


    http.csrf(httpSecurityCsrfConfigurer -> {
      httpSecurityCsrfConfigurer
          .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
          .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
          .ignoringRequestMatchers(
              PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.POST, "/api/v1/integration/rdf"),
              PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.POST, "/api/v1/integration/search/**")
          )
      ;
    });

    // Force CSRF token to be generated on every response
    http.addFilterAfter(new CsrfCookieFilter(), CsrfFilter.class);




    // TODO check if this works
    http.headers(httpSecurityHeadersConfigurer -> {
      httpSecurityHeadersConfigurer.frameOptions(frameOptionsConfig -> {
        frameOptionsConfig.sameOrigin();
      });
    });

    return http.build();

  }

  /**
   * Eagerly loads the CSRF token on every request so the cookie is always set.
   * Without this, Spring Security 6 defers token generation until something
   * explicitly accesses it (e.g., a Thymeleaf form rendering {@code ${_csrf.token}}).
   * JavaScript clients that read the CSRF cookie need it present before any
   * form page is visited.
   */
  static class CsrfCookieFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
        throws ServletException, IOException {
      CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
      if (csrfToken != null) {
        // Accessing the token value forces generation + cookie writing
        csrfToken.getToken();
      }
      filterChain.doFilter(request, response);
    }
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }


  /**
   * Method configures the logout process via open-id-connect
   * @return configured handler
   */
  private OidcClientInitiatedLogoutSuccessHandler oidcLogoutSuccessHandler() {
    var handler = new OidcClientInitiatedLogoutSuccessHandler(clientRegistrationRepository);
    handler.setPostLogoutRedirectUri("{baseUrl}/api/v1");
    return handler;
  }

}