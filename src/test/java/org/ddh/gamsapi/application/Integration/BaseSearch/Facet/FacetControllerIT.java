package org.ddh.gamsapi.application.Integration.BaseSearch.Facet;

import org.assertj.core.api.Assertions;
import org.ddh.gamsapi.TestUtilities.TestBag;
import org.ddh.gamsapi.TestUtilities.TestDigitalObject;
import org.ddh.gamsapi.TestUtilities.TestDublinCoreEntry;
import org.ddh.gamsapi.TestUtilities.TestProject;
import org.ddh.gamsapi.application.Ingest.Ingest;
import org.ddh.gamsapi.application.Ingest.interfaces.IIngestService;
import org.ddh.gamsapi.application.Ingest.utils.ZipUtils;
import org.ddh.gamsapi.application.Integration.SolrIntegrationTest;
import org.ddh.gamsapi.application.Integration.BaseSearch.BaseSearchService;
import org.ddh.gamsapi.domain.Project.ProjectBuilder;
import org.ddh.gamsapi.domain.Project.interfaces.IProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.data.auditing.AuditingHandler;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.io.File;
import java.io.IOException;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc(addFilters = false)
public class FacetControllerIT extends SolrIntegrationTest {

  @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
  @Autowired
  private MockMvc mockMvc;

  // disables auditing
  // (necessary -> otherwise the createdBy fields etc. from Project need to be filled)
  // this auditing / security test is done in a separate test
  @MockitoBean
  private AuditingHandler auditingHandler;

  @Autowired
  private BaseSearchService baseSearchService;

  @Autowired
  private IIngestService ingestService;

  @Autowired
  private IProjectRepository projectRepository;

  File bagFile;

  @BeforeEach
  public void setup() throws IOException {
    bagFile = TestBag.loadFile();
    projectRepository.save(ProjectBuilder.builder().projectAbbr(TestProject.PROJECT_ABBR.getValue()).build());

    // ingest the bag
    byte[] zippedBag = ZipUtils.zipDir(bagFile);
    Ingest ingest = new Ingest();
    ingest.setZippedBagItFolder(zippedBag);
    ingest.setProjectAbbr(TestProject.PROJECT_ABBR.getValue());
    ingestService.ingest(ingest);

    // index object
    baseSearchService.indexObject(
        TestProject.PROJECT_ABBR.getValue(), TestDigitalObject.DIGITAL_OBJECT_ID.getValue()
    );
  }

  @Nested
  public class FacetSearch {

    final String FACETED_SEARCH_BASE_URL = FacetController.FACET_SEARCH_PATH;
    String TEST_DC_URL_QUERY;
    String TEST_DC_SEARCH_REQUEST_URL;

    @BeforeEach
    public void setup() {
      TEST_DC_URL_QUERY = "dc.subject=Rumänisch";
      TEST_DC_SEARCH_REQUEST_URL = String.format("%s?projects=%s&%s",
          FACETED_SEARCH_BASE_URL,
          TestProject.PROJECT_ABBR.getValue(),
          TEST_DC_URL_QUERY
      );
    }

    @Test
    public void facetedSearchResponseIsNotEmptyOrNull() throws Exception {

      String response = mockMvc.perform(
              MockMvcRequestBuilders.get(TEST_DC_SEARCH_REQUEST_URL)
                  .accept(MediaType.APPLICATION_JSON)
          )
          .andExpect(status().isOk())
          .andReturn()
          .getResponse()
          .getContentAsString();

      Assertions.assertThat(response).isNotNull();
      Assertions.assertThat(response).isNotEmpty();


    }

    @Test
    public void facetSearchResponseContainsExpectedValues() throws Exception {
      String response = mockMvc.perform(
              MockMvcRequestBuilders.get(TEST_DC_SEARCH_REQUEST_URL)
                  .accept(MediaType.APPLICATION_JSON)
          )
          .andExpect(status().isOk())
          .andReturn()
          .getResponse()
          .getContentAsString();

      Assertions.assertThat(response)
          .contains(TestDublinCoreEntry.VALUE.getValue())
          .contains(TestDublinCoreEntry.NAME.getValue())
          .contains(TestProject.PROJECT_ABBR.getValue())
          .contains(TestDigitalObject.DIGITAL_OBJECT_ID.getValue())
          .contains(TestDublinCoreEntry.NAME.getValue())
      ;


    }

    @Test
    public void facetSearchForOnlyProjectReturnsExpectedValue() throws Exception {

      final String REQUEST_URL = String.format("%s?projects=%s",
          FACETED_SEARCH_BASE_URL,
          TestProject.PROJECT_ABBR.getValue()
      );

      String response = mockMvc.perform(
              MockMvcRequestBuilders.get(REQUEST_URL)
                  .accept(MediaType.APPLICATION_JSON)
          )
          .andExpect(status().isOk())
          .andReturn()
          .getResponse()
          .getContentAsString();

      Assertions.assertThat(response)
          .contains(TestProject.PROJECT_ABBR.getValue())
          .contains(TestDigitalObject.DIGITAL_OBJECT_ID.getValue());

    }

    @Test
    public void facetSearchWithFulltextQueryReturnsExpectedValue() throws Exception {

      final String FULLTEXT_QUERY = TestDublinCoreEntry.VALUE.getValue();
      final String REQUEST_URL = String.format("%s?projects=%s&q=%s",
          FACETED_SEARCH_BASE_URL,
          TestProject.PROJECT_ABBR.getValue(),
          FULLTEXT_QUERY
      );

      String response = mockMvc.perform(
              MockMvcRequestBuilders.get(REQUEST_URL)
                  .accept(MediaType.APPLICATION_JSON)
          )
          .andExpect(status().isOk())
          .andReturn()
          .getResponse()
          .getContentAsString();

      Assertions.assertThat(response)
          .contains(TestProject.PROJECT_ABBR.getValue())
          .contains(TestDigitalObject.DIGITAL_OBJECT_ID.getValue())
          .contains(FULLTEXT_QUERY);

    }


  }

  @Nested
  public class Webclient {

    @Test
    public void containsExpectedValues() throws Exception {

      final String FULLTEXT_SEARCH_URL = String.format("%s?projects=%s", FacetController.FACET_SEARCH_PATH,
          TestProject.PROJECT_ABBR.getValue()
      );

      String facetWebclientResponse = mockMvc.perform(
              MockMvcRequestBuilders.get(FULLTEXT_SEARCH_URL)
                  .accept(MediaType.TEXT_HTML_VALUE)
          )
          .andExpect(status().isOk())
          .andReturn()
          .getResponse()
          .getContentAsString();

      Assertions.assertThat(facetWebclientResponse)
          .isNotEmpty()
          .contains(
              "Faceted Search",
              TestDigitalObject.DIGITAL_OBJECT_ID.getValue(),
              TestProject.PROJECT_ABBR.getValue(),
              TestDublinCoreEntry.VALUE.getValue()
          );


    }



  }

}
