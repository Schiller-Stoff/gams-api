package org.ddh.gamsapi.infrastructure.System.security;

import org.ddh.gamsapi.IntegrationTest;
import org.ddh.gamsapi.TestUtilities.TestProject;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@AutoConfigureMockMvc
public class CsrfProtectionIT extends IntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Test
  public void csrfExemptedEndpoint_postWithoutCsrfToken_isRejectedByCsrf() throws Exception {
    mockMvc.perform(
            MockMvcRequestBuilders.post("/api/integration/v1/rdf")
                .content("SELECT * WHERE { ?s ?p ?o }")
        )
        .andExpect(MockMvcResultMatchers.status().is(Matchers.is(403)));
  }

  @Test
  public void csrfExemptedSearchEndpoint_postWithoutCsrfToken_isRejectedByCsrf() throws Exception {
    mockMvc.perform(
            MockMvcRequestBuilders.post("/api/integration/v1/search/some-query")
                .content("")
        )
        .andExpect(MockMvcResultMatchers.status().is(Matchers.is(403)));
  }

  /**
   * Verifies that non-exempted state-changing endpoints ARE protected by CSRF.
   * A POST without a CSRF token must be rejected with 403 Forbidden.
   */
  @Test
  public void csrfProtectedEndpoint_postWithoutCsrfToken_returns403() throws Exception {

    String testProjectAdminRole = GAMSAPIAuthorities.convertToRole(
        GAMSAPIAuthorities.getProjectAdmin(TestProject.PROJECT_ABBR.getValue())
    );

    mockMvc.perform(
            MockMvcRequestBuilders.post("/api/curation/v1/projects/test/objects")
                .content(new byte[0])
                .with(SecurityMockMvcRequestPostProcessors
                    .user("SOME_USER")
                    .roles(testProjectAdminRole)
                )
        )
        .andExpect(MockMvcResultMatchers.status().isForbidden());
  }

  /**
   * Verifies that the same endpoint succeeds when a valid CSRF token is provided.
   */
  @Test
  public void csrfProtectedEndpoint_postWithCsrfToken_isNotRejectedByCsrf() throws Exception {

    String testProjectAdminRole = GAMSAPIAuthorities.convertToRole(
        GAMSAPIAuthorities.getProjectAdmin(TestProject.PROJECT_ABBR.getValue())
    );

    mockMvc.perform(
            MockMvcRequestBuilders.post("/api/curation/v1/projects/test/objects")
                .content(new byte[0])
                .with(SecurityMockMvcRequestPostProcessors
                    .user("SOME_USER")
                    .roles(testProjectAdminRole)
                )
                .with(SecurityMockMvcRequestPostProcessors.csrf())
        )
        .andExpect(MockMvcResultMatchers.status().is(Matchers.not(403)));
  }
}
