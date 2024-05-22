package org.zim.gamsapi.System.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.zim.gamsapi.IntegrationTest;
import org.zim.gamsapi.System.configproperties.GAMSAPIProperties;
import org.zim.gamsapi.enums.TestProject;


/**
 * Tests expected authenication mechanisms for the application
 * Like all state changing operations require authentication.
 */
@AutoConfigureMockMvc
public class AuthenticationIT extends IntegrationTest {


  @Autowired
  private MockMvc mockMvc;

  @Test
  public void getRequestDoesntRequireAuthentication() throws Exception {
    final String PROJECTS_URL =  "/api/v1/projects/";
    mockMvc.perform(
      MockMvcRequestBuilders.get(PROJECTS_URL)
    ).andExpect(
      MockMvcResultMatchers.status().isOk()
    );
  }

  @Test
  public void headRequestDoesntRequireAuthentication() throws Exception {
    final String PROJECTS_URL =  "/api/v1/projects/";
    mockMvc.perform(
        MockMvcRequestBuilders.head(PROJECTS_URL)
    ).andExpect(
        MockMvcResultMatchers.status().isOk()
    );
  }

  @Test
  public void projectCreationRequiresAuthentication_redirects() throws Exception {
    final String PROJECT_CREATION_URL = "/api/v1/projects/" + TestProject.PROJECT_ABBR.getValue();
    // test works if redirected to oauth2 login page!
    mockMvc.perform(MockMvcRequestBuilders.put(PROJECT_CREATION_URL)
        // csrf would be needed if turned on.
        //.with(SecurityMockMvcRequestPostProcessors.csrf())
        .with(SecurityMockMvcRequestPostProcessors.anonymous()))
        // redirects to the oauth2 login page
        .andExpect(MockMvcResultMatchers.status().is3xxRedirection()
    );

  }

  @Test
  public void userCreationRequiresAuthentication_redirects() throws Exception {
    final String USER_CREATION_URL = "/api/v1/user/";
    mockMvc.perform(MockMvcRequestBuilders.post(USER_CREATION_URL)
        .with(SecurityMockMvcRequestPostProcessors.anonymous()))
        .andExpect(MockMvcResultMatchers.status().is3xxRedirection()
    );
  }

  @Test
  public void objectCreationRequiresAuthentication_redirects() throws Exception {
    final String USER_CREATION_URL = "/api/v1/projects/" + GAMSAPIProperties.DEMO_PROJECT_ABBR.name + "/objects/demo";
    mockMvc.perform(MockMvcRequestBuilders.put(USER_CREATION_URL)).andExpect(
            MockMvcResultMatchers.status().is3xxRedirection()
    );
  }

  @Test
  public void ingestRequiresAuthentication_redirects() throws Exception {
    final String INGEST_ENDPOINT =  "/api/v1/projects/" + GAMSAPIProperties.DEMO_PROJECT_ABBR.name + "/objects/";
    mockMvc.perform(MockMvcRequestBuilders.post(INGEST_ENDPOINT).content(new byte[0])
            .with(SecurityMockMvcRequestPostProcessors.anonymous()))
        .andExpect(MockMvcResultMatchers.status().is3xxRedirection());
  }

  @Test
  public void integrationApiPostDontRequireAuthentication_returns500() throws Exception {
    final String INTEGRATION_ENDPOINT =  "/api/v1/integration/rdf";

    mockMvc
        .perform(
            MockMvcRequestBuilders.post(INTEGRATION_ENDPOINT).content(new byte[0])
        )
        .andExpect(MockMvcResultMatchers.status().is5xxServerError());

  }

}
