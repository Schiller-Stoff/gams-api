package org.zim.gamsapi.System.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.util.MimeTypeUtils;

/**
 * Spring security configuration
 */
@Configuration
@Slf4j
@RequiredArgsConstructor
public class SpringSecurityConfiguration {

  private final UserProjectAuthorizationManager userProjectAuthorizationManager;

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

    http.authorizeHttpRequests(authorize -> {
      try {
        authorize
                // allow all GET requests
                .requestMatchers(HttpMethod.GET, "/**")
                .permitAll()
                // allow post requests against specific integration api endpoints (because: might get queries via POST)
                .requestMatchers(HttpMethod.POST,"/api/v1/integration/rdf*","/api/v1/integration/search*")
                .permitAll()
                // every state changing request needs authentication (POST / PUT / PATCH / DELETE)
                .requestMatchers(request -> !request.getMethod().equals(HttpMethod.GET.name()))
                .authenticated()
                // authorization: protect state changes against projects + users except if admin.
                .requestMatchers( HttpMethod.POST, ADMIN_ONLY_PATHS)
                .hasAnyAuthority(GAMSAPISecurityRoles.ADMINISTRATOR.name)
                .requestMatchers( HttpMethod.DELETE, ADMIN_ONLY_PATHS)
                .hasAnyAuthority(GAMSAPISecurityRoles.ADMINISTRATOR.name)
                .requestMatchers( HttpMethod.PATCH, ADMIN_ONLY_PATHS)
                .hasAnyAuthority(GAMSAPISecurityRoles.ADMINISTRATOR.name)
                .requestMatchers( HttpMethod.PUT, ADMIN_ONLY_PATHS)
                .hasAnyAuthority(GAMSAPISecurityRoles.ADMINISTRATOR.name)
                // configures: user must be assigned to project + have required roles (admin, editor,....) to change state of objects or datastreams (including ingest)
                .requestMatchers("/api/v1/projects/{projectAbbr}/objects/**", "/api/v1/integration/projects/{projectAbbr}/objects/**")
                .access(userProjectAuthorizationManager)
                //.anyRequest()
                //.authenticated()
                .and()
                .httpBasic()
                .and()
                .csrf()
                .ignoringRequestMatchers(request -> {
                  String acceptHeaderValue = request.getHeader(HttpHeaders.ACCEPT);
                  if(acceptHeaderValue == null) return true;
                  boolean containsTextHtml = acceptHeaderValue.contains(MimeTypeUtils.TEXT_HTML_VALUE);
                  // disable csrf for all requests that don't demand html = only html pages are csrf protected
                  return !containsTextHtml;
                })
                // allows to load e.g. datastream content directly via an embed / iframe tag.
                // https://stackoverflow.com/questions/28647136/how-to-disable-x-frame-options-response-header-in-spring-security
                .and()
                .headers()
                .frameOptions()
                .sameOrigin();
      } catch (Exception e) {
        String msg = String.format("Failed to correctly configure spring security - Might be an issue with CSRF protection settings %s", e);
        log.error(msg);
        throw new RuntimeException(e);
      }
    });

    return http.build();

  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

}
