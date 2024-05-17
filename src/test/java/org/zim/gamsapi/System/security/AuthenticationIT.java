package org.zim.gamsapi.System.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.zim.gamsapi.IntegrationTest;
import org.zim.gamsapi.System.configproperties.GAMSAPIProperties;


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
  public void projectCreationRequiresAuthentication() throws Exception {
    final String PROJECT_CREATION_URL = "/api/v1/projects/";
    // TODO update! would not return unauthorized but the redirect to the login page (of keycloak)
    // TODO why status 403? and not redirect?
    // TODO post request is not defined in the controller
    mockMvc.perform(MockMvcRequestBuilders.put(PROJECT_CREATION_URL)).andExpect(
            MockMvcResultMatchers.status().isUnauthorized()
    );;
  }

  @Test
  public void userCreationRequiresAuthentication() throws Exception {
    final String USER_CREATION_URL = "/api/v1/user/";
    // TODO update! would not return unauthorized but the redirect to the login page (of keycloak)
    // TODO why status 403? and not redirect?
    // TODO post request is not defined in the controller
    mockMvc.perform(MockMvcRequestBuilders.post(USER_CREATION_URL)).andExpect(
            MockMvcResultMatchers.status().isUnauthorized()
    );;
  }

  @Test
  public void objectCreationRequiresAuthentication() throws Exception {
    final String USER_CREATION_URL = "/api/v1/projects/" + GAMSAPIProperties.DEMO_PROJECT_ABBR.name + "/objects/demo";
    // TODO update! would not return unauthorized but the redirect to the login page (of keycloak)
    // TODO why status 403? and not redirect?
    // TODO post request is not defined in the controller
    mockMvc.perform(MockMvcRequestBuilders.put(USER_CREATION_URL)).andExpect(
            MockMvcResultMatchers.status().isUnauthorized()
    );
  }

}
