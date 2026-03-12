package org.ddh.gamsapi.infrastructure.User;

import org.ddh.gamsapi.UnitTest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class UserRoleProfileTest extends UnitTest {

  private static GrantedAuthority auth(String role) {
    return new SimpleGrantedAuthority(role);
  }

  @Nested
  class TopRoles {

    @Test
    void parsesSuperAdmin() {
      var profile = new UserRoleProfile(List.of(auth("ROLE_super_admin")));
      assertThat(profile.getTopRoles()).containsExactly("super_admin");
      assertThat(profile.hasTopRoles()).isTrue();
      assertThat(profile.hasProjectRoles()).isFalse();
    }

    @Test
    void parsesProjectsAdmin() {
      var profile = new UserRoleProfile(List.of(auth("ROLE_projects_admin")));
      assertThat(profile.getTopRoles()).containsExactly("projects_admin");
    }

    @Test
    void parsesBothTopRoles() {
      var profile = new UserRoleProfile(List.of(
          auth("ROLE_super_admin"),
          auth("ROLE_projects_admin")
      ));
      assertThat(profile.getTopRoles())
          .containsExactlyInAnyOrder("super_admin", "projects_admin");
    }

    @Test
    void filtersOutSpringInternalRoles() {
      var profile = new UserRoleProfile(List.of(
          auth("ROLE_super_admin"),
          auth("SCOPE_openid"),
          auth("SCOPE_profile"),
          auth("OIDC_USER")
      ));
      assertThat(profile.getTopRoles()).containsExactly("super_admin");
    }
  }

  @Nested
  class ProjectRoles {

    @Test
    void parsesProjectAdmin() {
      var profile = new UserRoleProfile(List.of(auth("ROLE_cantus_admin")));
      assertThat(profile.getProjectRoles()).containsKey("cantus");
      assertThat(profile.getProjectRoles().get("cantus")).containsExactly("admin");
    }

    @Test
    void parsesProjectEditor() {
      var profile = new UserRoleProfile(List.of(auth("ROLE_memo_editor")));
      assertThat(profile.getProjectRoles().get("memo")).containsExactly("editor");
    }

    @Test
    void parsesProjectViewer() {
      var profile = new UserRoleProfile(List.of(auth("ROLE_cantus_viewer")));
      assertThat(profile.getProjectRoles().get("cantus")).containsExactly("viewer");
    }

    @Test
    void parsesContentRestrictionRole() {
      var profile = new UserRoleProfile(List.of(
          auth("ROLE_roth_viewer_OVER_AGE_18")
      ));
      assertThat(profile.getProjectRoles().get("roth"))
          .containsExactly("viewer_OVER_AGE_18");
    }

    @Test
    void parsesMultipleRolesAcrossProjects() {
      var profile = new UserRoleProfile(List.of(
          auth("ROLE_memo_admin"),
          auth("ROLE_cantus_viewer"),
          auth("ROLE_memo_editor")
      ));
      assertThat(profile.getProjectRoles()).hasSize(2);
      assertThat(profile.getProjectRoles().get("memo"))
          .containsExactlyInAnyOrder("admin", "editor");
      assertThat(profile.getProjectRoles().get("cantus"))
          .containsExactly("viewer");
    }

    @Test
    void projectsAreSortedAlphabetically() {
      var profile = new UserRoleProfile(List.of(
          auth("ROLE_zephyr_admin"),
          auth("ROLE_alpha_viewer"),
          auth("ROLE_memo_editor")
      ));
      assertThat(profile.getProjectRoles().keySet())
          .containsExactly("alpha", "memo", "zephyr");
    }
  }

  @Nested
  class ProjectAbbrWithUnderscores {

    @Test
    void parsesProjectAbbrContainingUnderscore() {
      var profile = new UserRoleProfile(List.of(
          auth("ROLE_my_org_editor")
      ));
      // "my_org" is the project, "editor" is the role
      // Parser finds first _editor at index 6 → projectAbbr="my_org"
      assertThat(profile.getProjectRoles()).containsKey("my_org");
      assertThat(profile.getProjectRoles().get("my_org")).containsExactly("editor");
    }
  }

  @Nested
  class MixedRoles {

    @Test
    void parsesRealisticUserProfile() {
      var profile = new UserRoleProfile(List.of(
          auth("ROLE_super_admin"),
          auth("ROLE_memo_admin"),
          auth("ROLE_cantus_viewer"),
          auth("ROLE_roth_viewer_OVER_AGE_18"),
          auth("SCOPE_openid"),
          auth("SCOPE_profile")
      ));
      assertThat(profile.getTopRoles()).containsExactly("super_admin");
      assertThat(profile.getProjectRoles()).hasSize(3);
      assertThat(profile.getProjectRoles().get("memo")).containsExactly("admin");
      assertThat(profile.getProjectRoles().get("cantus")).containsExactly("viewer");
      assertThat(profile.getProjectRoles().get("roth")).containsExactly("viewer_OVER_AGE_18");
    }
  }

  @Nested
  class EdgeCases {

    @Test
    void emptyAuthorities() {
      var profile = new UserRoleProfile(List.of());
      assertThat(profile.hasTopRoles()).isFalse();
      assertThat(profile.hasProjectRoles()).isFalse();
    }

    @Test
    void onlySpringInternalRoles() {
      var profile = new UserRoleProfile(List.of(
          auth("SCOPE_openid"), auth("SCOPE_profile")
      ));
      assertThat(profile.hasTopRoles()).isFalse();
      assertThat(profile.hasProjectRoles()).isFalse();
    }

    @Test
    void unrecognizedRoleIsIgnored() {
      // A role that doesn't match top roles or project role suffixes
      var profile = new UserRoleProfile(List.of(
          auth("ROLE_some_random_thing")
      ));
      // Not a top role, no recognized suffix → ignored
      // Actually: "some_random_thing" — no _admin/_editor/_viewer → dropped
      assertThat(profile.hasTopRoles()).isFalse();
      assertThat(profile.hasProjectRoles()).isFalse();
    }
  }
}
