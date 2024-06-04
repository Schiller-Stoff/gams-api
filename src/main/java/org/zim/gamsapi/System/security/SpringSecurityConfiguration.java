package org.zim.gamsapi.System.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring security configuration
 */
@Configuration
@Slf4j
@RequiredArgsConstructor
public class SpringSecurityConfiguration {

  private final UserProjectAuthorizationManager userProjectAuthorizationManager;

  private final DatastreamContentAuthorizationManager datastreamContentAuthorizationManager;

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


    // configure oauth2 login
    // TODO elaborate oauth2login settings (e.g. login page, success url, failure url)
    http.oauth2Login(httpSecurityOAuth2LoginConfigurer -> {
      httpSecurityOAuth2LoginConfigurer
          //.loginPage("/login")
          //.defaultSuccessUrl("/home", true)
          .failureUrl("/login?error=true");
    });

    http.authorizeHttpRequests(auth ->
      auth
          // allow post requests against specific integration api endpoints (because: might get queries via POST)
          // TODO think about stricter security check (must be query for solr / sparql / deny if to big content etc.)
          .requestMatchers(HttpMethod.POST,"/api/v1/integration/rdf*","/api/v1/integration/search*")
          .permitAll()
          .requestMatchers(HttpMethod.GET, "/api/v1/projects/{projectAbbr}/objects/{id}/datastreams/{dsid}/content")
          .access(datastreamContentAuthorizationManager)
          // All HEAD and GET after above rules are allowed
          .requestMatchers(request -> {
              String requestMethod = request.getMethod();
              return switch (requestMethod) {
                case "GET", "HEAD" -> true;
                default -> false;
              };
            })
          .permitAll()
          // authorization only applies for these endpoints
          // TODO this endpoint is too broad - should be more specific / because atm only ingest is allowed.
          .requestMatchers("/api/v1/projects/{projectAbbr}/objects/**", "/api/v1/integration/projects/{projectAbbr}/objects/**")
          .access(userProjectAuthorizationManager)
          // projects may only be created / deleted by global admin role
          .requestMatchers(HttpMethod.PUT,"/api/v1/projects/{projectAbbr}/", "/api/v1/projects/{projectAbbr}")
          .hasAuthority(GAMSAPIAuthorities.getAdmin())
          .requestMatchers(HttpMethod.DELETE,"/api/v1/projects/{projectAbbr}/", "/api/v1/projects/{projectAbbr}")
          .hasAuthority(GAMSAPIAuthorities.getAdmin())
          // any not matched requests require authentication
          .anyRequest()
          .authenticated()

    );

    // TODO configure csrf protection?
    http.csrf(httpSecurityCsrfConfigurer -> {
      httpSecurityCsrfConfigurer.disable();
    });

    // TODO check if this works
    http.headers(httpSecurityHeadersConfigurer -> {
      httpSecurityHeadersConfigurer.frameOptions(frameOptionsConfig -> {
        frameOptionsConfig.sameOrigin();
      });
    });

    return http.build();

  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

}
