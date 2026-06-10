package org.ddh.gamsapi.infrastructure.System.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.ddh.gamsapi.IntegrationTest;

@AutoConfigureMockMvc
class CORSConfigurationIT extends IntegrationTest {

  @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
  @Autowired
  private MockMvc mockMvc;

  @Test
  void corsPreflightRequest_allowedOrigin_returnsProperHeaders() throws Exception {
    mockMvc.perform(MockMvcRequestBuilders.options("/api/curation/v1/projects")
            .header("Origin", "http://localhost:3000")
            .header("Access-Control-Request-Method", "GET")
            .header("Access-Control-Request-Headers", "Content-Type"))
        .andExpect(MockMvcResultMatchers.status().isOk())
        .andExpect(MockMvcResultMatchers.header().string("Access-Control-Allow-Origin", "http://localhost:3000"))
        .andExpect(MockMvcResultMatchers.header().exists("Access-Control-Allow-Methods"))
        .andExpect(MockMvcResultMatchers.header().exists("Access-Control-Allow-Headers"));
  }

  @Test
  void corsPreflightRequest_disallowedOrigin_noHeaders() throws Exception {
    mockMvc.perform(MockMvcRequestBuilders.options("/api/curation/v1/projects")
            .header("Origin", "http://malicious-site.com")
            .header("Access-Control-Request-Method", "GET"))
        .andExpect(MockMvcResultMatchers.status().isForbidden());
  }

  @Test
  void corsActualRequest_integrationEndpoint_allowsPublicAccess() throws Exception {
    mockMvc.perform(
        MockMvcRequestBuilders.post("/api/integration/v1/rdf")
            .with(SecurityMockMvcRequestPostProcessors.csrf())
            .header("Origin", "http://localhost:3000")
            .content(""))
        .andExpect(MockMvcResultMatchers.header().string("Access-Control-Allow-Origin", "http://localhost:3000"));
  }

  @Test
  void corsActualRequest_authenticatedEndpoint_requiresCredentials() throws Exception {
    mockMvc.perform(MockMvcRequestBuilders.get("/api/userinfo")
            .header("Origin", "http://localhost:3000"))
        .andExpect(MockMvcResultMatchers.header().string("Access-Control-Allow-Credentials", "true"));
  }
}