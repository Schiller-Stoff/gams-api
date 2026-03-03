package org.ddh.gamsapi.infrastructure.System.security;

import org.ddh.gamsapi.TestUtilities.TestProject;
import org.ddh.gamsapi.UnitTest;
import org.ddh.gamsapi.domain.Datastream.DatastreamAuthorizationService;
import org.ddh.gamsapi.infrastructure.System.security.exceptions.UserNotAuthorizedException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DatastreamAuthorizationServiceTest extends UnitTest {

  private final DatastreamAuthorizationService service = new DatastreamAuthorizationService();

  private static final String PROJECT_ABBR = TestProject.PROJECT_ABBR.getValue();

  // ==================================================================================
  // Helper methods
  // ==================================================================================

  private Authentication authenticatedUser(String... roles) {
    Authentication auth = mock(Authentication.class);
    when(auth.isAuthenticated()).thenReturn(true);
    List<GrantedAuthority> authorities = java.util.Arrays.stream(roles)
        .map(SimpleGrantedAuthority::new)
        .map(a -> (GrantedAuthority) a)
        .toList();
    when(auth.getAuthorities()).thenReturn((java.util.Collection) authorities);
    return auth;
  }

  private Authentication anonymousUser() {
    return mock(AnonymousAuthenticationToken.class);
  }

  // ==================================================================================
  // No content restrictions → public access
  // ==================================================================================

  @Nested
  public class UnrestrictedDatastreams {

    @Test
    public void grantsAccessWhenContentRestrictionsAreEmpty() {
      AuthorizationDecision decision = service.checkContentAccess(
          PROJECT_ABBR, Set.of(), null);
      assertThat(decision.isGranted()).isTrue();
    }

    @Test
    public void grantsAccessWhenContentRestrictionsAreNull() {
      AuthorizationDecision decision = service.checkContentAccess(
          PROJECT_ABBR, null, null);
      assertThat(decision.isGranted()).isTrue();
    }

    @Test
    public void grantsAnonymousAccessWhenUnrestricted() {
      AuthorizationDecision decision = service.checkContentAccess(
          PROJECT_ABBR, Set.of(), anonymousUser());
      assertThat(decision.isGranted()).isTrue();
    }
  }

  // ==================================================================================
  // Restricted datastreams — unauthenticated users
  // ==================================================================================

  @Nested
  public class RestrictedUnauthenticated {

    private final Set<String> RESTRICTIONS = Set.of("OVER_AGE_18");

    @Test
    public void deniesWhenAuthenticationIsNull() {
      AuthorizationDecision decision = service.checkContentAccess(
          PROJECT_ABBR, RESTRICTIONS, null);
      assertThat(decision.isGranted()).isFalse();
    }

    @Test
    public void deniesAnonymousUser() {
      AuthorizationDecision decision = service.checkContentAccess(
          PROJECT_ABBR, RESTRICTIONS, anonymousUser());
      assertThat(decision.isGranted()).isFalse();
    }

    @Test
    public void deniesUnauthenticatedUser() {
      Authentication auth = mock(Authentication.class);
      when(auth.isAuthenticated()).thenReturn(false);

      AuthorizationDecision decision = service.checkContentAccess(
          PROJECT_ABBR, RESTRICTIONS, auth);
      assertThat(decision.isGranted()).isFalse();
    }
  }

  // ==================================================================================
  // Restricted datastreams — privileged roles (always granted)
  // ==================================================================================

  @Nested
  public class PrivilegedRoles {

    private final Set<String> RESTRICTIONS = Set.of("OVER_AGE_18");

    @Test
    public void grantsSuperadmin() {
      Authentication auth = authenticatedUser(GAMSAPIAuthorities.getAdmin());
      AuthorizationDecision decision = service.checkContentAccess(
          PROJECT_ABBR, RESTRICTIONS, auth);
      assertThat(decision.isGranted()).isTrue();
    }

    @Test
    public void grantsProjectAdmin() {
      Authentication auth = authenticatedUser(
          GAMSAPIAuthorities.getProjectAdmin(PROJECT_ABBR));
      AuthorizationDecision decision = service.checkContentAccess(
          PROJECT_ABBR, RESTRICTIONS, auth);
      assertThat(decision.isGranted()).isTrue();
    }

    @Test
    public void grantsProjectEditor() {
      Authentication auth = authenticatedUser(
          GAMSAPIAuthorities.getProjectEditor(PROJECT_ABBR));
      AuthorizationDecision decision = service.checkContentAccess(
          PROJECT_ABBR, RESTRICTIONS, auth);
      assertThat(decision.isGranted()).isTrue();
    }

    @Test
    public void grantsProjectViewer() {
      Authentication auth = authenticatedUser(
          GAMSAPIAuthorities.getProjectViewer(PROJECT_ABBR));
      AuthorizationDecision decision = service.checkContentAccess(
          PROJECT_ABBR, RESTRICTIONS, auth);
      assertThat(decision.isGranted()).isTrue();
    }

    @Test
    public void grantsProjectAdminFromDifferentProjectDoesNotApply() {
      // admin of a DIFFERENT project should not grant access
      Authentication auth = authenticatedUser(
          GAMSAPIAuthorities.getProjectAdmin("other_project"));

      assertThatThrownBy(() -> service.checkContentAccess(
          PROJECT_ABBR, RESTRICTIONS, auth))
          .isInstanceOf(UserNotAuthorizedException.class);
    }
  }

  // ==================================================================================
  // Restricted datastreams — content restriction matching
  // ==================================================================================

  @Nested
  public class ContentRestrictionMatching {

    @Test
    public void grantsUserWithMatchingRestrictionRole() {
      String restrictedRole = GAMSAPIAuthorities
          .buildProjectViewerContentRestricted(PROJECT_ABBR, "OVER_AGE_18");

      Authentication auth = authenticatedUser(restrictedRole);
      AuthorizationDecision decision = service.checkContentAccess(
          PROJECT_ABBR, Set.of("OVER_AGE_18"), auth);
      assertThat(decision.isGranted()).isTrue();
    }

    @Test
    public void grantsWhenUserMatchesOneOfMultipleRestrictions() {
      // Datastream has two restrictions, user only matches one → OR logic → granted
      String restrictedRole = GAMSAPIAuthorities
          .buildProjectViewerContentRestricted(PROJECT_ABBR, "PROJECT_INTERNAL");

      Authentication auth = authenticatedUser(restrictedRole);
      AuthorizationDecision decision = service.checkContentAccess(
          PROJECT_ABBR,
          Set.of("OVER_AGE_18", "PROJECT_INTERNAL"),
          auth);
      assertThat(decision.isGranted()).isTrue();
    }

    @Test
    public void deniesUserWithNonMatchingRestrictionRole() {
      String wrongRestrictionRole = GAMSAPIAuthorities
          .buildProjectViewerContentRestricted(PROJECT_ABBR, "UNDER_AGE_18");

      Authentication auth = authenticatedUser(wrongRestrictionRole);

      assertThatThrownBy(() -> service.checkContentAccess(
          PROJECT_ABBR, Set.of("OVER_AGE_18"), auth))
          .isInstanceOf(UserNotAuthorizedException.class)
          .hasMessageContaining("OVER_AGE_18");
    }

    @Test
    public void deniesUserWithRestrictionRoleForDifferentProject() {
      // User has OVER_AGE_18 for project "other" but requests "roth"
      String wrongProjectRole = GAMSAPIAuthorities
          .buildProjectViewerContentRestricted("other", "OVER_AGE_18");

      Authentication auth = authenticatedUser(wrongProjectRole);

      assertThatThrownBy(() -> service.checkContentAccess(
          PROJECT_ABBR, Set.of("OVER_AGE_18"), auth))
          .isInstanceOf(UserNotAuthorizedException.class);
    }

    @Test
    public void deniesAuthenticatedUserWithNoRelevantRoles() {
      // User is authenticated but has completely unrelated roles
      Authentication auth = authenticatedUser("ROLE_some_unrelated_role");

      assertThatThrownBy(() -> service.checkContentAccess(
          PROJECT_ABBR, Set.of("OVER_AGE_18"), auth))
          .isInstanceOf(UserNotAuthorizedException.class);
    }
  }
}