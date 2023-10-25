package org.zim.gamsapi.System.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.zim.gamsapi.IntegrationTest;


/**
 * Tests expected authenication mechanisms for the application
 * Like all state changing operations require authentication.
 */
@AutoConfigureMockMvc
public class BasicAuthenticationIT extends IntegrationTest {


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
    mockMvc.perform(MockMvcRequestBuilders.post(PROJECT_CREATION_URL)).andExpect(
            MockMvcResultMatchers.status().isUnauthorized()
    );;
  }

  @Test
  public void userCreationRequiresAuthentication() throws Exception {
    final String USER_CREATION_URL = "/api/v1/user/";
    mockMvc.perform(MockMvcRequestBuilders.post(USER_CREATION_URL)).andExpect(
            MockMvcResultMatchers.status().isUnauthorized()
    );;
  }

}
