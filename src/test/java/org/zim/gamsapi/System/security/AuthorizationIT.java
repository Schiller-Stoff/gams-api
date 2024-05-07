package org.zim.gamsapi.System.security;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.zim.gamsapi.IntegrationTest;
import org.zim.gamsapi.System.configproperties.GAMSAPIProperties;
import org.zim.gamsapi.System.security.exceptions.UserNotAssignedToProjectException;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

/**
 * Tests Authorization settings in the application
 */
@AutoConfigureMockMvc
public class AuthorizationIT extends IntegrationTest {


  @Autowired
  private MockMvc mockMvc;

  @Test
  @Disabled
  public void ingestRequiresAuthorization() throws Exception {

    // currently disabled because --> need to think about

    final String INGEST_ENDPOINT =  "/api/v1/projects/" + GAMSAPIProperties.DEMO_PROJECT_ABBR.name + "/objects/test";
    mockMvc.perform(MockMvcRequestBuilders.post(INGEST_ENDPOINT).content(new byte[0])
            .with(user(GAMSAPIProperties.ADMIN_USER_NAME.name).roles(GAMSAPIProperties.DEMO_PROJECT_ABBR.name)))
            .andExpect(MockMvcResultMatchers.status().isOk());
  }

}
