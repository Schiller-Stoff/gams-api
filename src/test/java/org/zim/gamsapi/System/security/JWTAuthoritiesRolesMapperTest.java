package org.zim.gamsapi.System.security;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2UserAuthority;
import org.zim.gamsapi.System.security.exceptions.AuthorizationConfigurationException;
import org.zim.gamsapi.UnitTest;
import java.util.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class JWTAuthoritiesRolesMapperTest extends UnitTest {

  private JWTAuthoritiesRolesMapper mapper;

  final String TEST_KEYCLOAK_ROLE_CANTUS = "cantus" + GAMSAPIAuthorities.ROLE_DELIMITER.name + GAMSAPIAuthorities.ADMINISTRATOR.name;
  final String TEST_KEYCLOAK_ROLE_ADMIN = GAMSAPIAuthorities.ADMINISTRATOR.name;
  final String EXPECTED_ROLE_ADMIN = GAMSAPIAuthorities.ROLE_PREFIX.name + TEST_KEYCLOAK_ROLE_ADMIN;
  final String EXPECTED_ROLE_CANTUS = GAMSAPIAuthorities.ROLE_PREFIX.name + TEST_KEYCLOAK_ROLE_CANTUS;

  @BeforeEach
  public void setUp() {
    mapper = new JWTAuthoritiesRolesMapper();
  }

  @Test
  public void successfullyMapsProvidedOauth2Authorities() {

    Map<String, Object> realmAccess = new HashMap<>();
    realmAccess.put("roles", List.of(TEST_KEYCLOAK_ROLE_ADMIN, TEST_KEYCLOAK_ROLE_CANTUS));

    Map<String, Object> claims = new HashMap<>();
    claims.put("realm_access", realmAccess);

    OAuth2UserAuthority authority = new OAuth2UserAuthority("foo", claims);

    Collection<? extends GrantedAuthority> result = mapper.mapAuthorities(List.of(authority));

    assertEquals(2, result.size());

    org.assertj.core.api.Assertions.assertThat(result)
      .extracting(GrantedAuthority::getAuthority)
      .contains(EXPECTED_ROLE_ADMIN, EXPECTED_ROLE_CANTUS);

  }

  @Test
  public void throwsIfRealmAccessClaimNotAvailable() {
    Map<String, Object> claims = new HashMap<>();
    // roles are set
    claims.put("roles", List.of(TEST_KEYCLOAK_ROLE_ADMIN, TEST_KEYCLOAK_ROLE_CANTUS));
    OAuth2UserAuthority authority = new OAuth2UserAuthority("foo", claims);
    Assertions.assertThrows(AuthorizationConfigurationException.class, () -> {
      mapper.mapAuthorities(List.of(authority));
    });
  }

  @Test
  public void throwsIfRolesClaimNotAvailable() {
    Map<String, Object> claims = new HashMap<>();
    // roles are set
    claims.put("realm_access", "bar");
    OAuth2UserAuthority authority = new OAuth2UserAuthority("foo", claims);
    Assertions.assertThrows(AuthorizationConfigurationException.class, () -> {
      mapper.mapAuthorities(List.of(authority));
    });
  }


}