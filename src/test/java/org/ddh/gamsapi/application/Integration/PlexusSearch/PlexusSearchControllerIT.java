package org.ddh.gamsapi.application.Integration.PlexusSearch;

import org.assertj.core.api.Assertions;
import org.ddh.gamsapi.TestUtilities.TestBag;
import org.ddh.gamsapi.TestUtilities.TestDigitalObject;
import org.ddh.gamsapi.TestUtilities.TestProject;
import org.ddh.gamsapi.application.Ingest.interfaces.IIngestService;
import org.ddh.gamsapi.application.Ingest.utils.ZipUtils;
import org.ddh.gamsapi.application.Integration.Common.utils.solr.SolrDocument;
import org.ddh.gamsapi.application.Integration.Common.utils.solr.SolrGamsCores;
import org.ddh.gamsapi.application.Integration.SolrIntegrationTest;
import org.ddh.gamsapi.domain.Project.ProjectBuilder;
import org.ddh.gamsapi.domain.Project.interfaces.IProjectRepository;
import org.ddh.gamsapi.infrastructure.System.security.IUserPrincipalAuditorMapping;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.auditing.AuditingHandler;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.Set;

@AutoConfigureMockMvc(addFilters = false)
public class PlexusSearchControllerIT extends SolrIntegrationTest {


  @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
  @Autowired
  private MockMvc mockMvc;

  /**
   * Classes need to mock authenticated users when changing datastreams
   */
  @MockitoBean
  private AuditingHandler auditingHandler;
  @MockitoBean
  private IUserPrincipalAuditorMapping userPrincipalAuditorMapping;

  @Autowired
  private IIngestService ingestService;

  @Autowired
  private IProjectRepository projectRepository;

  @Autowired
  private PlexusSearchService plexusSearchService;

  File bagFile;


  @BeforeEach
  public void setup() throws IOException {
    Mockito.when(userPrincipalAuditorMapping.getCurrentAuditor())
        .thenReturn(Optional.of("test-user"));

    bagFile = TestBag.loadFile();
    projectRepository.save(ProjectBuilder.builder().projectAbbr(TestProject.PROJECT_ABBR.getValue()).build());

    // ingest the bag
    byte[] zippedBag = ZipUtils.zipDir(bagFile);
    ingestService.ingest(
        TestProject.PROJECT_ABBR.getValue(),
        new ByteArrayInputStream(zippedBag)
    );

  }

  @Nested
  public class IndexProjectObjects {

    @Test
    public void indexCreatesAtLeast1SolrDocument() throws Exception {
      // when
      mockMvc.perform(
              org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                  .post(PlexusSearchController.PLEXUS_SEARCH_MANAGEMENT_PATH,
                      TestProject.PROJECT_ABBR.getValue())
          )
          .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());

      // then
      var numDocs = solrClient.countDocumentsByPropertyValues(
          SolrGamsCores.PLEXUS_SEARCH_CORE.value,
          PlexusSearchProperties.ENTITY_PROJECT_ABBR.name,
          Set.of(TestProject.PROJECT_ABBR.getValue())
      );

      Assertions.assertThat(numDocs)
          .withFailMessage("Number of documents returned should be greater than 0")
          .isGreaterThan(0)
      ;

    }


  }

  @Nested
  public class IndexProjectObject {

    @Test
    public void indexSingleCreates1SolrDocument() throws Exception {
      // when
      mockMvc.perform(
              org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                  .post(PlexusSearchController.PLEXUS_SEARCH_SINGLE_OBJECT_MANAGEMENT_PATH,
                      TestProject.PROJECT_ABBR.getValue(),
                      TestDigitalObject.DIGITAL_OBJECT_ID.getValue()
                  )
          )
          .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());

      // then
      var numDocs = solrClient.countDocumentsByPropertyValues(
          SolrGamsCores.PLEXUS_SEARCH_CORE.value,
          PlexusSearchProperties.ENTITY_PROJECT_ABBR.name,
          Set.of(TestProject.PROJECT_ABBR.getValue())
      );

      Assertions.assertThat(numDocs)
          .withFailMessage("Number of documents returned should be equal to 1")
          .isEqualTo(1)
      ;

    }

  }

  @Nested
  public class DeleteProjectObjects {

    @Test
    public void deleteRemovesAllSolrDocuments() throws Exception {
      // given
      // first index some documents
      final String TEST_SOLR_DOCUMENT_ID = "test.1111111";

      // first fill data of plexus search core
      SolrDocument testSolrDocument = new SolrDocument();
      testSolrDocument.addProperty(PlexusSearchProperties.ENTITY_ID.name, TEST_SOLR_DOCUMENT_ID);
      testSolrDocument.addProperty(PlexusSearchProperties.ENTITY_PROJECT_ABBR.name, TestProject.PROJECT_ABBR.getValue());
      testSolrDocument.addProperty(PlexusSearchProperties.ENTITY_OBJECT_ID.name, TestDigitalObject.DIGITAL_OBJECT_ID.getValue());
      // posting data to solr
      solrClient.post(SolrGamsCores.PLEXUS_SEARCH_CORE.value, testSolrDocument);

      var numDocsAfterIndexing = solrClient.countDocumentsByPropertyValues(
          SolrGamsCores.PLEXUS_SEARCH_CORE.value,
          PlexusSearchProperties.ENTITY_PROJECT_ABBR.name,
          Set.of(TestProject.PROJECT_ABBR.getValue())
      );

      Assertions.assertThat(numDocsAfterIndexing)
          .withFailMessage("Number of documents after indexing should be greater than 0")
          .isGreaterThan(0)
      ;

      // when
      mockMvc.perform(
              org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                  .delete(PlexusSearchController.PLEXUS_SEARCH_MANAGEMENT_PATH,
                      TestProject.PROJECT_ABBR.getValue())
          )
          .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());

      // then
      var numDocsAfterDeletion = solrClient.countDocumentsByPropertyValues(
          SolrGamsCores.PLEXUS_SEARCH_CORE.value,
          PlexusSearchProperties.ENTITY_PROJECT_ABBR.name,
          Set.of(TestProject.PROJECT_ABBR.getValue())
      );

      Assertions.assertThat(numDocsAfterDeletion)
          .withFailMessage("Number of documents after deletion should be equal to 0")
          .isEqualTo(0)
      ;

    }

  }

  @Nested
  public class DeleteProjectObject {

    @Test
    public void deleteRemovesSingleSolrDocument() throws Exception {
      // given
      // first index some documents
      final String TEST_SOLR_DOCUMENT_ID = "test.1111111";

      // first fill data of plexus search core
      SolrDocument testSolrDocument = new SolrDocument();
      testSolrDocument.addProperty(PlexusSearchProperties.ENTITY_ID.name, TEST_SOLR_DOCUMENT_ID);
      testSolrDocument.addProperty(PlexusSearchProperties.ENTITY_PROJECT_ABBR.name, TestProject.PROJECT_ABBR.getValue());
      testSolrDocument.addProperty(PlexusSearchProperties.ENTITY_OBJECT_ID.name, TestDigitalObject.DIGITAL_OBJECT_ID.getValue());
      // posting data to solr
      solrClient.post(SolrGamsCores.PLEXUS_SEARCH_CORE.value, testSolrDocument);

      var numDocsAfterIndexing = solrClient.countDocumentsByPropertyValues(
          SolrGamsCores.PLEXUS_SEARCH_CORE.value,
          PlexusSearchProperties.ENTITY_PROJECT_ABBR.name,
          Set.of(TestProject.PROJECT_ABBR.getValue())
      );

      Assertions.assertThat(numDocsAfterIndexing)
          .withFailMessage("Number of documents after indexing should be greater than 0")
          .isGreaterThan(0)
      ;

      // when
      mockMvc.perform(
              org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                  .delete(PlexusSearchController.PLEXUS_SEARCH_SINGLE_OBJECT_MANAGEMENT_PATH,
                      TestProject.PROJECT_ABBR.getValue(),
                      TestDigitalObject.DIGITAL_OBJECT_ID.getValue()
                  )
          )
          .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());

      // then
      var numDocsAfterDeletion = solrClient.countDocumentsByPropertyValues(
          SolrGamsCores.PLEXUS_SEARCH_CORE.value,
          PlexusSearchProperties.ENTITY_PROJECT_ABBR.name,
          Set.of(TestProject.PROJECT_ABBR.getValue())
      );

      Assertions.assertThat(numDocsAfterDeletion)
          .withFailMessage("Number of documents after deletion should be equal to 0")
          .isEqualTo(0)
      ;

    }

  }

  @Nested
  public class Search {

    @Nested
    public class SearchPOST {
      @Test
      public void veryBasicSearchReturns1IndexedObject() throws Exception {
        // given
        plexusSearchService.indexObjects(TestProject.PROJECT_ABBR.getValue());

        final String SIMPLE_SEARCH_QUERY = String.format("%s:%s",
            PlexusSearchProperties.ENTITY_OBJECT_ID.name,
            TestDigitalObject.DIGITAL_OBJECT_ID.getValue()
        );

        String requestBody = String.format("""
          {
            "query": "%s"
          }
          """, SIMPLE_SEARCH_QUERY);

        // when
        var response = mockMvc.perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                    .post(PlexusSearchController.PLEXUS_SEARCH_GET_PATH)
                    .param("project", TestProject.PROJECT_ABBR.getValue())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody)
            )
            .andExpect(
                MockMvcResultMatchers.status().isOk()
            )
            .andReturn()
            .getResponse()
            .getContentAsString();


        // then
        Assertions.assertThat(response)
            .withFailMessage("Search response should contain at least one result")
            .contains("\"totalCount\":1")
        ;

      }
    }

    @Nested
    public class SearchGET {

      @Test
      public void veryBasicSearchReturns1IndexedObject() throws Exception {
        // given
        plexusSearchService.indexObjects(TestProject.PROJECT_ABBR.getValue());

        final String SIMPLE_SEARCH_QUERY = String.format("%s:%s",
            PlexusSearchProperties.ENTITY_OBJECT_ID.name,
            TestDigitalObject.DIGITAL_OBJECT_ID.getValue()
        );

        // when
        var response = mockMvc.perform(
                MockMvcRequestBuilders
                    .get(PlexusSearchController.PLEXUS_SEARCH_GET_PATH)
                    .param("project", TestProject.PROJECT_ABBR.getValue())
                    .param("q", SIMPLE_SEARCH_QUERY)
                    .contentType(MediaType.APPLICATION_JSON)
            )
            .andExpect(
                MockMvcResultMatchers.status().isOk()
            )
            .andReturn()
            .getResponse()
            .getContentAsString();

        // then
        Assertions.assertThat(response)
            .withFailMessage("Search response should contain at least one result")
            .contains("\"totalCount\":1");

      }

      @Test
      public void complexSearchWorksAsExpected() throws Exception {

        // given
        plexusSearchService.indexObjects(TestProject.PROJECT_ABBR.getValue());

        final String SIMPLE_SEARCH_QUERY = String.format("%s:%s",
            PlexusSearchProperties.ENTITY_OBJECT_ID.name,
            TestDigitalObject.DIGITAL_OBJECT_ID.getValue()
        );

        // when
        var response = mockMvc.perform(
                MockMvcRequestBuilders
                    .get(PlexusSearchController.PLEXUS_SEARCH_GET_PATH)
                    .param("project", TestProject.PROJECT_ABBR.getValue())
                    .param("q", SIMPLE_SEARCH_QUERY)
                    .param("start", "0")
                    .param("rows", "10")
                    .param("sort", "id asc")
                    .param("fq", String.format("%s:%s",
                        PlexusSearchProperties.ENTITY_PROJECT_ABBR.name,
                        TestProject.PROJECT_ABBR.getValue()
                    ))
                    .param("highlight", "true")
                    .param("highlightFields", PlexusSearchProperties.ENTITY_OBJECT_ID.name)
                    .param("highlightSnippetSize", "150")
                    .param("facetFields", PlexusSearchProperties.ENTITY_PROJECT_ABBR.name)
                    .param("facetLimit", "5")
                    .param("facetMinCount", "1")
                    .param("debug", "true")
                    .param("fl", PlexusSearchProperties.ENTITY_ID.name)
                    .contentType(MediaType.APPLICATION_JSON)
            )
            .andExpect(
                MockMvcResultMatchers.status().isOk()
            )
            .andReturn()
            .getResponse()
            .getContentAsString();

        // then
        Assertions.assertThat(response)
            .withFailMessage("Search response should contain at least one result")
            .contains("\"totalCount\":1");


      }


    }




  }
}
