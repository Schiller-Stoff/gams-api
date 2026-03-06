package org.ddh.gamsapi.infrastructure.System.security;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.ddh.gamsapi.IntegrationTest;
import org.ddh.gamsapi.infrastructure.System.configproperties.GAMSAPIProperties;
import org.ddh.gamsapi.TestUtilities.TestProject;


/**
 * Tests expected authentication mechanisms for the application.
 * Like all state changing operations require authentication.
 * <p>
 * With the login/logout URL rework, unauthenticated users are redirected
 * to {@code /api/auth/login} instead of the Spring default {@code /login}.
 * Auth infrastructure lives under {@code /api/auth/}, separate from the
 * versioned domain API at {@code /api/v1/}.
 */
@AutoConfigureMockMvc
public class AuthenticationIT extends IntegrationTest {


  @Autowired
  private MockMvc mockMvc;

  @Test
  public void getRequestDoesntRequireAuthentication() throws Exception {
    final String PROJECTS_URL =  "/api/v1/projects";
    mockMvc.perform(
        MockMvcRequestBuilders.get(PROJECTS_URL)
    ).andExpect(
        MockMvcResultMatchers.status().isOk()
    );
  }

  @Test
  public void headRequestDoesntRequireAuthentication() throws Exception {
    final String PROJECTS_URL =  "/api/v1/projects";
    mockMvc.perform(
        MockMvcRequestBuilders.head(PROJECTS_URL)
    ).andExpect(
        MockMvcResultMatchers.status().isOk()
    );
  }

  @Test
  public void projectCreationRequiresAuthentication_redirectsToLogin() throws Exception {
    final String PROJECT_CREATION_URL = "/api/v1/projects/" + TestProject.PROJECT_ABBR.getValue();
    mockMvc.perform(MockMvcRequestBuilders.put(PROJECT_CREATION_URL)
            .with(SecurityMockMvcRequestPostProcessors.csrf())
            .with(SecurityMockMvcRequestPostProcessors.anonymous()))
        .andExpect(MockMvcResultMatchers.status().is3xxRedirection())
        .andExpect(MockMvcResultMatchers.redirectedUrl("/api/auth/login")
        );
  }

  @Test
  public void userCreationRequiresAuthentication_redirectsToLogin() throws Exception {
    final String USER_CREATION_URL = "/api/v1/user/";
    mockMvc.perform(MockMvcRequestBuilders.post(USER_CREATION_URL)
            .with(SecurityMockMvcRequestPostProcessors.anonymous())
            .with(SecurityMockMvcRequestPostProcessors.csrf())
        )
        .andExpect(MockMvcResultMatchers.status().is3xxRedirection())
        .andExpect(MockMvcResultMatchers.redirectedUrl("/api/auth/login")
        );
  }

  @Test
  public void objectCreationRequiresAuthentication_redirectsToLogin() throws Exception {
    final String USER_CREATION_URL = "/api/v1/projects/" + GAMSAPIProperties.DEMO_PROJECT_ABBR.name + "/objects/demo";
    mockMvc.perform(
        MockMvcRequestBuilders.put(USER_CREATION_URL)
            .with(SecurityMockMvcRequestPostProcessors.csrf())
    ).andExpect(
        MockMvcResultMatchers.status().is3xxRedirection()
    ).andExpect(
        MockMvcResultMatchers.redirectedUrl("/api/auth/login")
    );
  }

  @Test
  public void ingestRequiresAuthentication_redirectsToLogin() throws Exception {
    final String INGEST_ENDPOINT =  "/api/v1/projects/" + GAMSAPIProperties.DEMO_PROJECT_ABBR.name + "/objects/";
    mockMvc.perform(MockMvcRequestBuilders.post(INGEST_ENDPOINT).content(new byte[0])
            .with(SecurityMockMvcRequestPostProcessors.anonymous())
            .with(SecurityMockMvcRequestPostProcessors.csrf())
        )
        .andExpect(MockMvcResultMatchers.status().is3xxRedirection())
        .andExpect(MockMvcResultMatchers.redirectedUrl("/api/auth/login"));
  }

  @Test
  @Disabled("Succeeds in IDE but fails in CI/CD pipeline, needs investigation")
  public void integrationApiPostDontRequireAuthentication_returns500() throws Exception {
    final String INTEGRATION_ENDPOINT =  "/api/v1/integration/rdf";

    mockMvc
        .perform(
            MockMvcRequestBuilders.post(INTEGRATION_ENDPOINT).content(new byte[0])
        )
        .andExpect(MockMvcResultMatchers.status().is5xxServerError());

  }

}