package org.ddh.gamsapi.application.Integration.SemanticSearch;

import org.ddh.gamsapi.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc(addFilters = false)
class SemanticSearchControllerIT extends IntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private SemanticSearchService semanticSearchService;

  @Test
  void indexProjectObjects_ShouldReturn200OK() throws Exception {
    mockMvc.perform(post("/api/v1/integration/semantic-search/projects/TEST/objects"))
        .andExpect(status().isOk());

    verify(semanticSearchService).indexObjects("TEST");
  }

  @Test
  void deleteProjectObjects_ShouldReturn200OK() throws Exception {
    mockMvc.perform(delete("/api/v1/integration/semantic-search/projects/TEST/objects"))
        .andExpect(status().isOk());

    verify(semanticSearchService).deleteIndexedObjects("TEST");
  }
}