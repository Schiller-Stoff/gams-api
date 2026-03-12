package org.ddh.gamsapi.infrastructure.User;

import org.ddh.gamsapi.IntegrationTest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for the user profile page at /api/v1/auth/profile.
 * Security filters are enabled (no addFilters=false) to test authentication
 * behavior with real Spring Security processing.
 */
@AutoConfigureMockMvc
public class UserRoleProfileControllerIT extends IntegrationTest {

  private static final String PROFILE_URL = "/api/auth/user";

  @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
  @Autowired
  private MockMvc mockMvc;

  // ==================================================================================
  // Unauthenticated access
  // ==================================================================================

  @Nested
  public class UnauthenticatedAccess {

    @Test
    public void redirectsToLoginWhenNotAuthenticated() throws Exception {
      mockMvc.perform(
              MockMvcRequestBuilders.get(PROFILE_URL)
                  .with(SecurityMockMvcRequestPostProcessors.anonymous())
          )
          .andExpect(status().is3xxRedirection())
          .andExpect(MockMvcResultMatchers.redirectedUrl("/api/auth/login"));
    }
  }

  // ==================================================================================
  // Authenticated access — page rendering
  // ==================================================================================

  @Nested
  public class AuthenticatedAccess {

    @Test
    public void returnsProfilePageForAuthenticatedUser() throws Exception {
      mockMvc.perform(
              MockMvcRequestBuilders.get(PROFILE_URL)
                  .with(oidcLogin()
                      .idToken(token -> token
                          .claim("sub", "test-user-id")
                          .claim("preferred_username", "testuser")
                          .claim("name", "Test User")
                          .claim("email", "test@example.com")
                      )
                  )
          )
          .andExpect(status().isOk())
          .andExpect(MockMvcResultMatchers.view().name("auth/profile"))
          .andExpect(MockMvcResultMatchers.content().contentType("text/html;charset=UTF-8"));
    }

    @Test
    public void displaysUsernameAndEmail() throws Exception {
      MvcResult result = mockMvc.perform(
              MockMvcRequestBuilders.get(PROFILE_URL)
                  .with(oidcLogin()
                      .idToken(token -> token
                          .claim("sub", "user-123")
                          .claim("preferred_username", "sebastian")
                          .claim("name", "Sebastian Stoff")
                          .claim("email", "sebastian@example.com")
                      )
                  )
          )
          .andExpect(status().isOk())
          .andReturn();

      String html = result.getResponse().getContentAsString();
      assertThat(html).contains("Sebastian Stoff");
      assertThat(html).contains("sebastian@example.com");
    }
  }

  // ==================================================================================
  // Top roles rendering
  // ==================================================================================

  @Nested
  public class TopRolesDisplay {

    @Test
    public void displaysSuperAdminRole() throws Exception {
      MvcResult result = mockMvc.perform(
              MockMvcRequestBuilders.get(PROFILE_URL)
                  .with(oidcLogin()
                      .idToken(token -> token.claim("sub", "admin-user"))
                      .authorities(List.of(
                          new SimpleGrantedAuthority("ROLE_super_admin"),
                          new SimpleGrantedAuthority("SCOPE_openid")
                      ))
                  )
          )
          .andExpect(status().isOk())
          .andReturn();

      String html = result.getResponse().getContentAsString();
      assertThat(html).contains("Global Roles");
      assertThat(html).contains("super_admin");
    }

    @Test
    public void displaysBothTopRoles() throws Exception {
      MvcResult result = mockMvc.perform(
              MockMvcRequestBuilders.get(PROFILE_URL)
                  .with(oidcLogin()
                      .idToken(token -> token.claim("sub", "admin-user"))
                      .authorities(List.of(
                          new SimpleGrantedAuthority("ROLE_super_admin"),
                          new SimpleGrantedAuthority("ROLE_projects_admin")
                      ))
                  )
          )
          .andExpect(status().isOk())
          .andReturn();

      String html = result.getResponse().getContentAsString();
      assertThat(html).contains("super_admin");
      assertThat(html).contains("projects_admin");
    }

    @Test
    public void doesNotShowGlobalRolesSectionWhenUserHasNone() throws Exception {
      MvcResult result = mockMvc.perform(
              MockMvcRequestBuilders.get(PROFILE_URL)
                  .with(oidcLogin()
                      .idToken(token -> token.claim("sub", "regular-user"))
                      .authorities(List.of(
                          new SimpleGrantedAuthority("ROLE_cantus_viewer")
                      ))
                  )
          )
          .andExpect(status().isOk())
          .andReturn();

      String html = result.getResponse().getContentAsString();
      assertThat(html).doesNotContain("Global Roles");
    }
  }

  // ==================================================================================
  // Project roles rendering
  // ==================================================================================

  @Nested
  public class ProjectRolesDisplay {

    @Test
    public void displaysProjectWithAdminRole() throws Exception {
      MvcResult result = mockMvc.perform(
              MockMvcRequestBuilders.get(PROFILE_URL)
                  .with(oidcLogin()
                      .idToken(token -> token.claim("sub", "project-user"))
                      .authorities(List.of(
                          new SimpleGrantedAuthority("ROLE_memo_admin")
                      ))
                  )
          )
          .andExpect(status().isOk())
          .andReturn();

      String html = result.getResponse().getContentAsString();
      assertThat(html).contains("My Projects");
      assertThat(html).contains("memo");
      assertThat(html).contains("admin");
    }

    @Test
    public void displaysMultipleProjectsWithDifferentRoles() throws Exception {
      MvcResult result = mockMvc.perform(
              MockMvcRequestBuilders.get(PROFILE_URL)
                  .with(oidcLogin()
                      .idToken(token -> token.claim("sub", "multi-project-user"))
                      .authorities(List.of(
                          new SimpleGrantedAuthority("ROLE_memo_admin"),
                          new SimpleGrantedAuthority("ROLE_cantus_viewer"),
                          new SimpleGrantedAuthority("ROLE_roth_editor")
                      ))
                  )
          )
          .andExpect(status().isOk())
          .andReturn();

      String html = result.getResponse().getContentAsString();
      assertThat(html).contains("memo");
      assertThat(html).contains("cantus");
      assertThat(html).contains("roth");
    }

    @Test
    public void displaysContentRestrictionRole() throws Exception {
      MvcResult result = mockMvc.perform(
              MockMvcRequestBuilders.get(PROFILE_URL)
                  .with(oidcLogin()
                      .idToken(token -> token.claim("sub", "restricted-user"))
                      .authorities(List.of(
                          new SimpleGrantedAuthority("ROLE_roth_viewer_OVER_AGE_18")
                      ))
                  )
          )
          .andExpect(status().isOk())
          .andReturn();

      String html = result.getResponse().getContentAsString();
      assertThat(html).contains("roth");
      assertThat(html).contains("viewer_OVER_AGE_18");
    }

    @Test
    public void projectNameLinksToProjectPage() throws Exception {
      MvcResult result = mockMvc.perform(
              MockMvcRequestBuilders.get(PROFILE_URL)
                  .with(oidcLogin()
                      .idToken(token -> token.claim("sub", "linked-user"))
                      .authorities(List.of(
                          new SimpleGrantedAuthority("ROLE_cantus_editor")
                      ))
                  )
          )
          .andExpect(status().isOk())
          .andReturn();

      String html = result.getResponse().getContentAsString();
      assertThat(html).contains("/api/v1/projects/cantus");
    }

    @Test
    public void doesNotShowProjectsSectionWhenUserHasNone() throws Exception {
      MvcResult result = mockMvc.perform(
              MockMvcRequestBuilders.get(PROFILE_URL)
                  .with(oidcLogin()
                      .idToken(token -> token.claim("sub", "admin-only"))
                      .authorities(List.of(
                          new SimpleGrantedAuthority("ROLE_super_admin")
                      ))
                  )
          )
          .andExpect(status().isOk())
          .andReturn();

      String html = result.getResponse().getContentAsString();
      assertThat(html).doesNotContain("My Projects");
    }
  }

  // ==================================================================================
  // Mixed roles — realistic scenarios
  // ==================================================================================

  @Nested
  public class RealisticScenarios {

    @Test
    public void displaysFullProfileForUserWithMixedRoles() throws Exception {
      MvcResult result = mockMvc.perform(
              MockMvcRequestBuilders.get(PROFILE_URL)
                  .with(oidcLogin()
                      .idToken(token -> token
                          .claim("sub", "power-user")
                          .claim("name", "Power User")
                          .claim("email", "power@uni-graz.at")
                      )
                      .authorities(List.of(
                          new SimpleGrantedAuthority("ROLE_projects_admin"),
                          new SimpleGrantedAuthority("ROLE_memo_admin"),
                          new SimpleGrantedAuthority("ROLE_memo_editor"),
                          new SimpleGrantedAuthority("ROLE_cantus_viewer"),
                          new SimpleGrantedAuthority("ROLE_roth_viewer_OVER_AGE_18"),
                          new SimpleGrantedAuthority("SCOPE_openid"),
                          new SimpleGrantedAuthority("SCOPE_profile")
                      ))
                  )
          )
          .andExpect(status().isOk())
          .andReturn();

      String html = result.getResponse().getContentAsString();

      // User info
      assertThat(html).contains("Power User");
      assertThat(html).contains("power@uni-graz.at");

      // Top roles
      assertThat(html).contains("Global Roles");
      assertThat(html).contains("projects_admin");

      // Project roles
      assertThat(html).contains("My Projects");
      assertThat(html).contains("memo");
      assertThat(html).contains("cantus");
      assertThat(html).contains("roth");

      // Spring internal roles should NOT appear
      assertThat(html).doesNotContain("SCOPE_openid");
      assertThat(html).doesNotContain("SCOPE_profile");
    }

    @Test
    public void displaysNoRolesMessageForUserWithOnlySpringInternalRoles() throws Exception {
      MvcResult result = mockMvc.perform(
              MockMvcRequestBuilders.get(PROFILE_URL)
                  .with(oidcLogin()
                      .idToken(token -> token.claim("sub", "empty-user"))
                      .authorities(List.of(
                          new SimpleGrantedAuthority("SCOPE_openid"),
                          new SimpleGrantedAuthority("SCOPE_profile")
                      ))
                  )
          )
          .andExpect(status().isOk())
          .andReturn();

      String html = result.getResponse().getContentAsString();
      assertThat(html).contains("don't have any roles assigned");
      assertThat(html).doesNotContain("Global Roles");
      assertThat(html).doesNotContain("My Projects");
    }
  }
}
