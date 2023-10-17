package org.zim.gamsapi.System.security;

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
 * Conditionally enables spring security for different profiles.
 */
@Configuration
@Slf4j
public class SpringSecurityConfiguration {

  /**
   * Combined spring security matchers.
   * Matches all endpoints that require an admin authorization
   * (e.g. used along restrictions to DELETE / POST requests.)
   */
  private final String[] ALL_ADMIN_AUTH_MATCHER = {"/api/v1/user**", "/api/v1/projects/**"};

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

    log.info("*** Initializing spring security config ***");

    http.authorizeHttpRequests(authorize -> {
      try {
        authorize
                // only admin is allowed to change projects + users
                // but: allow all GET requests
                .requestMatchers(HttpMethod.GET, ALL_ADMIN_AUTH_MATCHER)
                .permitAll()
                // auth protect state changes except if admin.
                .requestMatchers( HttpMethod.POST, ALL_ADMIN_AUTH_MATCHER)
                .hasAnyAuthority(SecurityRoles.ADMINISTRATOR.name)
                .requestMatchers( HttpMethod.DELETE, ALL_ADMIN_AUTH_MATCHER)
                .hasAnyAuthority(SecurityRoles.ADMINISTRATOR.name)
                .requestMatchers( HttpMethod.PATCH, ALL_ADMIN_AUTH_MATCHER)
                .hasAnyAuthority(SecurityRoles.ADMINISTRATOR.name)
                .requestMatchers( HttpMethod.PUT, ALL_ADMIN_AUTH_MATCHER)
                .hasAnyAuthority(SecurityRoles.ADMINISTRATOR.name)

                // any request that is not defined in patterns before will require authentication!
                .anyRequest()
                .authenticated()

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
                });
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
