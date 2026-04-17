package org.ddh.gamsapi.application.Integration.CustomSearch;

import org.assertj.core.api.Assertions;
import org.ddh.gamsapi.TestUtilities.TestBag;
import org.ddh.gamsapi.TestUtilities.TestDigitalObject;
import org.ddh.gamsapi.TestUtilities.TestProject;
import org.ddh.gamsapi.application.Ingest.interfaces.IIngestService;
import org.ddh.gamsapi.application.Ingest.utils.ZipUtils;
import org.ddh.gamsapi.application.Integration.SolrIntegrationTest;
import org.ddh.gamsapi.application.Integration.Common.utils.solr.SolrGamsCores;
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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.Set;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc(addFilters = false)
public class CustomSearchControllerIT extends SolrIntegrationTest {

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
  private CustomSearchService customSearchService;

  @Autowired
  private IIngestService ingestService;

  @Autowired
  private IProjectRepository projectRepository;

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
  public class IndexObjects {

    @Test
    public void customIndexCreatesAtLeastOneDocumentInFulltextCore() throws Exception {

      int fulltextCoreDocumentCountInitial = solrClient.countDocumentsByPropertyValues(
          SolrGamsCores.CUSTOM_SEARCH_CORE.value,
          CustomSearchProperties.ENTITY_PROJECT_ABBR.name,
          Set.of(TestProject.PROJECT_ABBR.getValue())
      );
      // at first fulltext core should be empty
      Assertions.assertThat(fulltextCoreDocumentCountInitial).isEqualTo(0);

      mockMvc.perform(
              MockMvcRequestBuilders.post(
                      CustomSearchController.CUSTOM_SEARCH_MANAGEMENT_PATH,
                      TestProject.PROJECT_ABBR.getValue()
                  )
                  .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk());

      int fulltextCoreDocumentCount = solrClient.countDocumentsByPropertyValues(
          SolrGamsCores.CUSTOM_SEARCH_CORE.value,
          CustomSearchProperties.ENTITY_PROJECT_ABBR.name,
          Set.of(TestProject.PROJECT_ABBR.getValue())
      );

      Assertions.assertThat(fulltextCoreDocumentCount)
          .isGreaterThan(0);

    }

  }

  @Nested
  public class IndexObject {

    @Test
    public void indexSingleObjectCreatesExpectedDocumentInFulltextCore() throws Exception {

      int fulltextCoreDocumentCountInitial = solrClient.countDocumentsByPropertyValues(
          SolrGamsCores.CUSTOM_SEARCH_CORE.value,
          CustomSearchProperties.ENTITY_PROJECT_ABBR.name,
          Set.of(TestProject.PROJECT_ABBR.getValue())
      );
      // at first fulltext core should be empty
      Assertions.assertThat(fulltextCoreDocumentCountInitial).isEqualTo(0);

      mockMvc.perform(
              MockMvcRequestBuilders.post(
                      CustomSearchController.CUSTOM_SEARCH_SINGLE_OBJECT_MANAGEMENT_PATH,
                      TestProject.PROJECT_ABBR.getValue(),
                      TestDigitalObject.DIGITAL_OBJECT_ID.getValue()
                  )
                  .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk());

      String response = solrClient.retrieveSolrDocumentByProperty(
          SolrGamsCores.CUSTOM_SEARCH_CORE.value, "objectId", TestDigitalObject.DIGITAL_OBJECT_ID.getValue()
      );

      int solrDocumentCount = solrClient.countDocumentsByPropertyValues(
          SolrGamsCores.CUSTOM_SEARCH_CORE.value,
          CustomSearchProperties.ENTITY_PROJECT_ABBR.name,
          Set.of(TestProject.PROJECT_ABBR.getValue())
      );

      Assertions.assertThat(solrDocumentCount)
          .isGreaterThan(0);

      Assertions.assertThat(response)
          .isNotNull()
          .contains("\"objectId\":\""+ TestDigitalObject.DIGITAL_OBJECT_ID.getValue());

    }

  }

  @Nested
  public class DeleteIndexedObjects {

    @Test
    public void customDeleteRemovesAllProjectDocumentsFromFulltextCore() throws Exception {

      customSearchService.indexObjects(TestProject.PROJECT_ABBR.getValue());

      // first index some documents
      int fulltextCoreDocumentCountInitial = solrClient.countDocumentsByPropertyValues(
          SolrGamsCores.CUSTOM_SEARCH_CORE.value,
          CustomSearchProperties.ENTITY_PROJECT_ABBR.name,
          Set.of(TestProject.PROJECT_ABBR.getValue())
      );

      Assertions.assertThat(fulltextCoreDocumentCountInitial)
          .isGreaterThan(0);


      // now delete the indexed documents
      mockMvc.perform(
              MockMvcRequestBuilders.delete(
                      CustomSearchController.CUSTOM_SEARCH_MANAGEMENT_PATH,
                      TestProject.PROJECT_ABBR.getValue()
                  )
                  .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk());

      int fulltextCoreDocumentCountAfterDelete = solrClient.countDocumentsByPropertyValues(
          SolrGamsCores.CUSTOM_SEARCH_CORE.value,
          CustomSearchProperties.ENTITY_PROJECT_ABBR.name,
          Set.of(TestProject.PROJECT_ABBR.getValue())
      );

      Assertions.assertThat(fulltextCoreDocumentCountAfterDelete)
          .isEqualTo(0);

    }

  }

  @Nested
  public class DeleteIndexedObject {

    @Test
    public void deleteSingleIndexedObjectDecrementsDocumentCountFromFulltextCore() throws Exception {

      // first index some documents
      customSearchService.indexObjects(TestProject.PROJECT_ABBR.getValue());

      int fulltextCoreDocumentCountInitial = solrClient.countDocumentsByPropertyValues(
          SolrGamsCores.CUSTOM_SEARCH_CORE.value,
          CustomSearchProperties.ENTITY_PROJECT_ABBR.name,
          Set.of(TestProject.PROJECT_ABBR.getValue())
      );

      Assertions.assertThat(fulltextCoreDocumentCountInitial)
          .isGreaterThan(0);

      // now delete the single indexed document
      mockMvc.perform(
              MockMvcRequestBuilders.delete(
                      CustomSearchController.CUSTOM_SEARCH_SINGLE_OBJECT_MANAGEMENT_PATH,
                      TestProject.PROJECT_ABBR.getValue(),
                      TestDigitalObject.DIGITAL_OBJECT_ID.getValue()
                  )
                  .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk());

      int fulltextCoreDocumentCountAfterDelete = solrClient.countDocumentsByPropertyValues(
          SolrGamsCores.CUSTOM_SEARCH_CORE.value,
          CustomSearchProperties.ENTITY_PROJECT_ABBR.name,
          Set.of(TestProject.PROJECT_ABBR.getValue())
      );

      Assertions.assertThat(fulltextCoreDocumentCountAfterDelete)
          .isEqualTo(fulltextCoreDocumentCountInitial - 1);

    }
  }


  @Nested
  public class SearchEntities {

    @Test
    public void simpleFulltextSearchReturnContainsExpectedValues() throws Exception {

      int fulltextCoreDocumentCountInitial = solrClient.countDocumentsByPropertyValues(
          SolrGamsCores.CUSTOM_SEARCH_CORE.value,
          CustomSearchProperties.ENTITY_PROJECT_ABBR.name,
          Set.of(TestProject.PROJECT_ABBR.getValue())
      );
      // at first fulltext core should be empty
      Assertions.assertThat(fulltextCoreDocumentCountInitial).isEqualTo(0);

      customSearchService.indexObjects(TestProject.PROJECT_ABBR.getValue());

      String response = mockMvc.perform(
              MockMvcRequestBuilders.get(
                      CustomSearchController.CUSTOM_SEARCH_GET_PATH
                  )
                  .param("project", TestProject.PROJECT_ABBR.getValue())
                  .param("q", "")
                  .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andReturn().getResponse().getContentAsString();

      Assertions.assertThat(response)
          .isNotEmpty()
          .isNotNull()
          .contains(
              TestProject.PROJECT_ABBR.getValue(),
              TestDigitalObject.DIGITAL_OBJECT_ID.getValue()
          );

    }

    @Test
    public void simpleFulltextSearchWithPaginationReturnContainsExpectedValues() throws Exception {

      int fulltextCoreDocumentCountInitial = solrClient.countDocumentsByPropertyValues(
          SolrGamsCores.CUSTOM_SEARCH_CORE.value,
          CustomSearchProperties.ENTITY_PROJECT_ABBR.name,
          Set.of(TestProject.PROJECT_ABBR.getValue())
      );
      // at first fulltext core should be empty
      Assertions.assertThat(fulltextCoreDocumentCountInitial).isEqualTo(0);

      customSearchService.indexObjects(TestProject.PROJECT_ABBR.getValue());

      String response = mockMvc.perform(
              MockMvcRequestBuilders.get(
                      CustomSearchController.CUSTOM_SEARCH_GET_PATH
                  )
                  .param("project", TestProject.PROJECT_ABBR.getValue())
                  .param("q", "")
                  .param("pageIndex", "0")
                  .param("pageSize", "10")
                  .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andReturn().getResponse().getContentAsString();

      Assertions.assertThat(response)
          .isNotEmpty()
          .isNotNull()
          .contains(
              TestProject.PROJECT_ABBR.getValue(),
              TestDigitalObject.DIGITAL_OBJECT_ID.getValue()
          );

    }

    @Test
    public void complexFulltextSearchReturnContainsExpectedValues() throws Exception {

      int fulltextCoreDocumentCountInitial = solrClient.countDocumentsByPropertyValues(
          SolrGamsCores.CUSTOM_SEARCH_CORE.value,
          CustomSearchProperties.ENTITY_PROJECT_ABBR.name,
          Set.of(TestProject.PROJECT_ABBR.getValue())
      );
      // at first fulltext core should be empty
      Assertions.assertThat(fulltextCoreDocumentCountInitial).isEqualTo(0);

      customSearchService.indexObjects(TestProject.PROJECT_ABBR.getValue());

      String response = mockMvc.perform(
              MockMvcRequestBuilders.get(
                      CustomSearchController.CUSTOM_SEARCH_GET_PATH
                  )
                  .param("project", TestProject.PROJECT_ABBR.getValue())
                  .param("q", "")
                  .param("tag", "test")
                  .param("pageIndex", "0")
                  .param("pageSize", "10")
                  .param("sortBy", "id")
                  .param("sortDir", "asc")
                  .param("startDate", "2015-09-01T00:00:00Z")
                  .param("endDate", "2015-09-01T23:59:59Z")
                  .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andReturn().getResponse().getContentAsString();

      Assertions.assertThat(response)
          .isNotEmpty()
          .isNotNull()
          .contains(
              TestProject.PROJECT_ABBR.getValue(),
              TestDigitalObject.DIGITAL_OBJECT_ID.getValue()
          );

    }

    @Test
    public void fulltextSearchWithUnkownTagReturnsNoResults() throws Exception {

      int fulltextCoreDocumentCountInitial = solrClient.countDocumentsByPropertyValues(
          SolrGamsCores.CUSTOM_SEARCH_CORE.value,
          CustomSearchProperties.ENTITY_PROJECT_ABBR.name,
          Set.of(TestProject.PROJECT_ABBR.getValue())
      );
      // at first fulltext core should be empty
      Assertions.assertThat(fulltextCoreDocumentCountInitial).isEqualTo(0);

      customSearchService.indexObjects(TestProject.PROJECT_ABBR.getValue());

      String response = mockMvc.perform(
              MockMvcRequestBuilders.get(
                      CustomSearchController.CUSTOM_SEARCH_GET_PATH
                  )
                  .param("project", TestProject.PROJECT_ABBR.getValue())
                  .param("q", "")
                  .param("tag", "thisTagDoesNotExistInIndex")
                  .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andReturn().getResponse().getContentAsString();

      Assertions.assertThat(response)
          .isNotEmpty()
          .isNotNull()
          .contains("\"totalElements\":0");

    }

  }

}
