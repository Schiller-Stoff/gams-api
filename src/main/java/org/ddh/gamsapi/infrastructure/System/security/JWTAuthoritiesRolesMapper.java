package org.ddh.gamsapi.infrastructure.System.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.security.oauth2.core.user.OAuth2UserAuthority;
import org.springframework.stereotype.Component;
import org.ddh.gamsapi.infrastructure.System.security.exceptions.AuthorizationConfigurationException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Maps the roles from the oauth2 token (JWT) - e.g. configured via keycloak -
 * to the spring security authorities (roles are authorities with the ROLE_ prefix).
 * https://www.baeldung.com/spring-boot-keycloak
 * TODO write tests for this class
 */
@Component
@Slf4j
public class JWTAuthoritiesRolesMapper implements GrantedAuthoritiesMapper {

  private static final String GROUPS = "groups";
  private static final String REALM_ACCESS_CLAIM = "realm_access";
  private static final String ROLES_CLAIM = "roles";

  @Override
  public Collection<? extends GrantedAuthority> mapAuthorities(Collection<? extends GrantedAuthority> authorities) {

    Set<GrantedAuthority> mappedAuthorities = new HashSet<>();
    var authority = authorities.iterator().next();
    boolean isOidc = authority instanceof OidcUserAuthority;

    if (isOidc) {
      log.trace("Mapping authorities for OIDC user");
      var oidcUserAuthority = (OidcUserAuthority) authority;
      var userInfo = oidcUserAuthority.getUserInfo();

      // Tokens can be configured to return roles under
      // Groups or REALM ACCESS hence have to check both
      // at GAMS only realm_access is used
      if (userInfo.hasClaim(REALM_ACCESS_CLAIM)) {
        var realmAccess = userInfo.getClaimAsMap(REALM_ACCESS_CLAIM);
        Collection<String> roles;
        try {
          roles = (Collection<String>) realmAccess.get(ROLES_CLAIM);
        } catch (ClassCastException e) {
          String msg = "Roles claim is not a collection of strings. Please configure keycloak to include roles in the token!";
          log.error(msg);
          throw new AuthorizationConfigurationException(msg);
        }

        mappedAuthorities.addAll(generateAuthoritiesFromClaim(roles));
        log.trace("Successfully mapped jwt authorities to spring security authorities: " + mappedAuthorities);
      } else if (userInfo.hasClaim(GROUPS)) {
        String msg = "User has no realm access claim but groups claim. Please configure keycloak to include roles in the token! (activate mapper and assign role)";
        log.error(msg);
        throw new AuthorizationConfigurationException(msg);
      } else {
        String msg = "User has no realm access claim. Please configure keycloak to include roles in the token! (activate mapper and assign role)";
        log.error(msg);
        throw new AuthorizationConfigurationException(msg);
      }
    } else {
      log.trace("Mapping authorities for OAuth2 user");
      var oauth2UserAuthority = (OAuth2UserAuthority) authority;
      Map<String, Object> userAttributes = oauth2UserAuthority.getAttributes();

      if (userAttributes.containsKey(REALM_ACCESS_CLAIM)) {
        // extract realm access claim
        Map<String, Object> realmAccess;
        try {
          realmAccess = (Map<String, Object>) userAttributes.get(REALM_ACCESS_CLAIM);
        } catch (ClassCastException e) {
          String msg = "Realm access claim is not a map. Please configure keycloak to include roles in the token!";
          log.error(msg);
          throw new AuthorizationConfigurationException(msg);
        }
        // extract roles from realm access claim
        Collection<String> roles;
        try {
          roles = (Collection<String>) realmAccess.get(ROLES_CLAIM);
        } catch (ClassCastException e) {
          String msg = "Roles claim is not a collection of strings. Please configure keycloak to include roles in the token!";
          log.error(msg);
          throw new AuthorizationConfigurationException(msg);
        }

        mappedAuthorities.addAll(generateAuthoritiesFromClaim(roles));
        log.trace("Successfully mapped jwt authorities to spring security authorities: " + mappedAuthorities);
      } else {
        String msg = "User has no realm access claim. Please configure keycloak to include roles in the token!";
        log.error(msg);
        throw new AuthorizationConfigurationException(msg);
      }
    }
    return mappedAuthorities;

  }

  Collection<GrantedAuthority> generateAuthoritiesFromClaim(Collection<String> roles) {
    return roles.stream().map(role -> new SimpleGrantedAuthority("ROLE_" + role)).collect(
        Collectors.toList());
  }

}
