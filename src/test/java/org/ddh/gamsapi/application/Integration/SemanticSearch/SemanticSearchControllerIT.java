package org.ddh.gamsapi.application.Integration.SemanticSearch;

import org.ddh.gamsapi.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc(addFilters = false)
class SemanticSearchControllerIT extends IntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private SemanticSearchService semanticSearchService;

  private static final String PROJECT_ABBR = "TEST";
  private static final String OBJECT_ID = "o:test.1";
  private static final String PROJECT_PATH = "/api/v1/integration/semantic-search/projects/" + PROJECT_ABBR + "/objects";
  private static final String SINGLE_OBJECT_PATH = PROJECT_PATH + "/" + OBJECT_ID;

  // ---------------------------------------------------------------------------
  // Project-level Operations
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName("POST /projects/{projectAbbr}/objects (JSON) - Should return 200 OK")
  void indexProjectObjects_ShouldReturn200OK() throws Exception {
    mockMvc.perform(post(PROJECT_PATH)
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk());

    verify(semanticSearchService).indexObjects(PROJECT_ABBR);
  }

  @Test
  @DisplayName("POST /projects/{projectAbbr}/objects (HTML) - Should redirect to project objects page")
  void indexProjectObjectsHtml_ShouldRedirect() throws Exception {
    mockMvc.perform(post(PROJECT_PATH)
            .accept(MediaType.TEXT_HTML))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/api/v1/projects/" + PROJECT_ABBR + "/objects"));

    verify(semanticSearchService).indexObjects(PROJECT_ABBR);
  }

  @Test
  @DisplayName("DELETE /projects/{projectAbbr}/objects (JSON) - Should return 200 OK")
  void deleteProjectObjects_ShouldReturn200OK() throws Exception {
    mockMvc.perform(delete(PROJECT_PATH)
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk());

    verify(semanticSearchService).deleteIndexedObjects(PROJECT_ABBR);
  }

  @Test
  @DisplayName("DELETE /projects/{projectAbbr}/objects (HTML) - Should redirect to project objects page")
  void deleteProjectObjectsHtml_ShouldRedirect() throws Exception {
    mockMvc.perform(delete(PROJECT_PATH)
            .accept(MediaType.TEXT_HTML))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/api/v1/projects/" + PROJECT_ABBR + "/objects"));

    verify(semanticSearchService).deleteIndexedObjects(PROJECT_ABBR);
  }

  // ---------------------------------------------------------------------------
  // Single Object Operations
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName("POST /projects/{projectAbbr}/objects/{id} (JSON) - Should return 200 OK")
  void indexObject_ShouldReturn200OK() throws Exception {
    mockMvc.perform(post(SINGLE_OBJECT_PATH)
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk());

    verify(semanticSearchService).indexObject(PROJECT_ABBR, OBJECT_ID);
  }

  @Test
  @DisplayName("POST /projects/{projectAbbr}/objects/{id} (HTML) - Should redirect to object page")
  void indexObjectHtml_ShouldRedirect() throws Exception {
    mockMvc.perform(post(SINGLE_OBJECT_PATH)
            .accept(MediaType.TEXT_HTML))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/api/v1/projects/" + PROJECT_ABBR + "/objects/" + OBJECT_ID));

    verify(semanticSearchService).indexObject(PROJECT_ABBR, OBJECT_ID);
  }

  @Test
  @DisplayName("DELETE /projects/{projectAbbr}/objects/{id} (JSON) - Should return 200 OK")
  void deleteObject_ShouldReturn200OK() throws Exception {
    mockMvc.perform(delete(SINGLE_OBJECT_PATH)
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk());

    verify(semanticSearchService).deleteIndexedObject(PROJECT_ABBR, OBJECT_ID);
  }

  @Test
  @DisplayName("DELETE /projects/{projectAbbr}/objects/{id} (HTML) - Should redirect to object page")
  void deleteObjectHtml_ShouldRedirect() throws Exception {
    mockMvc.perform(delete(SINGLE_OBJECT_PATH)
            .accept(MediaType.TEXT_HTML))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/api/v1/projects/" + PROJECT_ABBR + "/objects/" + OBJECT_ID));

    verify(semanticSearchService).deleteIndexedObject(PROJECT_ABBR, OBJECT_ID);
  }
}