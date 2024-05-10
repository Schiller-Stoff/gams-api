package org.zim.gamsapi.System.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.security.oauth2.core.user.OAuth2UserAuthority;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Spring security configuration
 */
@Configuration
@Slf4j
@RequiredArgsConstructor
public class SpringSecurityConfiguration {

  private final Oauth2AuthorizationManager userProjectAuthorizationManager;

  /**
   * Combined spring security matchers.
   * Matches all endpoints that require an admin authorization
   * (e.g. used along restrictions to DELETE / POST requests.)
   */
  private final String[] ADMIN_ONLY_PATHS = {"/api/v1/user**", "/api/v1/projects/{projectAbbr}"};

  private final String[] PUBLIC_GET_PATHS = {"/api/v1**", "/api/v1/**"};


  private static final String GROUPS = "groups";
  private static final String REALM_ACCESS_CLAIM = "realm_access";
  private static final String ROLES_CLAIM = "roles";

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

    log.info("*** Initializing spring security config ***");


    // configure oauth2 login
    http.oauth2Login(httpSecurityOAuth2LoginConfigurer -> {
      httpSecurityOAuth2LoginConfigurer
          //.loginPage("/login")
          //.defaultSuccessUrl("/home", true)
          .failureUrl("/login?error=true");
    });


    http.authorizeHttpRequests(auth ->
      auth
          .requestMatchers("/**")
          .access(userProjectAuthorizationManager)
          // TODO means that every request needs ouath2 login - not suitable for public endpoints
          .anyRequest()
          .authenticated()
    );

    return http.build();

  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }


  /**
   * Maps the authorities from the token to the authorities in the application.
   * https://www.baeldung.com/spring-boot-keycloak
   * @return GrantedAuthoritiesMapper
   */
  @Bean
  public GrantedAuthoritiesMapper userAuthoritiesMapperForKeycloak() {

    return authorities -> {
      //TODO remove demo logging
      log.error("******** CALLING GRANTED AUTHORITIES MAPPER ********");
      Set<GrantedAuthority> mappedAuthorities = new HashSet<>();
      var authority = authorities.iterator().next();
      boolean isOidc = authority instanceof OidcUserAuthority;


      if (isOidc) {
        var oidcUserAuthority = (OidcUserAuthority) authority;
        var userInfo = oidcUserAuthority.getUserInfo();

        log.error("********** USER INFO (in mapper)" + userInfo.getClaims());
        log.error("************ USER AUTHORITY: " + oidcUserAuthority.getAuthority());

        // TODO needs suitable keycloak configuration (include roles in token!)

        // Tokens can be configured to return roles under
        // Groups or REALM ACCESS hence have to check both
        if (userInfo.hasClaim(REALM_ACCESS_CLAIM)) {
          var realmAccess = userInfo.getClaimAsMap(REALM_ACCESS_CLAIM);
          var roles = (Collection<String>) realmAccess.get(ROLES_CLAIM);
          mappedAuthorities.addAll(generateAuthoritiesFromClaim(roles));
        } else if (userInfo.hasClaim(GROUPS)) {

          // TODO remove mapping of groups! (raise an error if REALM_ACCESS_CLAIM is not present in token!)
          // (so that admins could propperly configure keycloak)

          Collection<String> roles = (Collection<String>) userInfo.getClaim(
              GROUPS);
          mappedAuthorities.addAll(generateAuthoritiesFromClaim(roles));
        }
      } else {
        var oauth2UserAuthority = (OAuth2UserAuthority) authority;
        Map<String, Object> userAttributes = oauth2UserAuthority.getAttributes();

        log.error("********** USER ATTRIBUTES (in mapper)" + userAttributes);

        if (userAttributes.containsKey(REALM_ACCESS_CLAIM)) {
          Map<String, Object> realmAccess = (Map<String, Object>) userAttributes.get(
              REALM_ACCESS_CLAIM);
          Collection<String> roles = (Collection<String>) realmAccess.get(ROLES_CLAIM);
          mappedAuthorities.addAll(generateAuthoritiesFromClaim(roles));
        }
      }
      return mappedAuthorities;
    };
  }

  Collection<GrantedAuthority> generateAuthoritiesFromClaim(Collection<String> roles) {
    return roles.stream().map(role -> new SimpleGrantedAuthority("ROLE_" + role)).collect(
        Collectors.toList());
  }

}
