package org.zim.gamsapi.System.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.zim.gamsapi.IntegrationTest;

@AutoConfigureMockMvc
public class CORSConfigurationIT extends IntegrationTest {

  @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
  @Autowired
  private MockMvc mockMvc;

  @Test
  public void corsPreflightRequest_allowedOrigin_returnsProperHeaders() throws Exception {
    mockMvc.perform(MockMvcRequestBuilders.options("/api/v1/projects")
            .header("Origin", "http://localhost:3000")
            .header("Access-Control-Request-Method", "GET")
            .header("Access-Control-Request-Headers", "Content-Type"))
        .andExpect(MockMvcResultMatchers.status().isOk())
        .andExpect(MockMvcResultMatchers.header().string("Access-Control-Allow-Origin", "http://localhost:3000"))
        .andExpect(MockMvcResultMatchers.header().exists("Access-Control-Allow-Methods"))
        .andExpect(MockMvcResultMatchers.header().exists("Access-Control-Allow-Headers"));
  }

  @Test
  public void corsPreflightRequest_disallowedOrigin_noHeaders() throws Exception {
    mockMvc.perform(MockMvcRequestBuilders.options("/api/v1/projects")
            .header("Origin", "http://malicious-site.com")
            .header("Access-Control-Request-Method", "GET"))
        .andExpect(MockMvcResultMatchers.status().isForbidden());
  }

  @Test
  public void corsActualRequest_integrationEndpoint_allowsPublicAccess() throws Exception {
    mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/integration/rdf")
            .header("Origin", "http://localhost:3000")
            .content(""))
        .andExpect(MockMvcResultMatchers.header().string("Access-Control-Allow-Origin", "http://localhost:3000"));
  }

  @Test
  public void corsActualRequest_authenticatedEndpoint_requiresCredentials() throws Exception {
    mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/userinfo")
            .header("Origin", "http://localhost:3000"))
        .andExpect(MockMvcResultMatchers.header().string("Access-Control-Allow-Credentials", "true"));
  }
}