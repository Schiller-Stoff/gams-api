package org.ddh.gamsapi.application.Integration.CustomSearch;

import org.assertj.core.api.Assertions;
import org.ddh.gamsapi.TestUtilities.TestBag;
import org.ddh.gamsapi.TestUtilities.TestProject;
import org.ddh.gamsapi.application.Ingest.Ingest;
import org.ddh.gamsapi.application.Ingest.interfaces.IIngestService;
import org.ddh.gamsapi.application.Ingest.utils.ZipUtils;
import org.ddh.gamsapi.application.Integration.BaseSearch.BaseSearchIntegrationTest;
import org.ddh.gamsapi.application.Integration.Common.utils.solr.SolrGamsCores;
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
import java.util.Set;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc(addFilters = false)
public class CustomSearchControllerIT extends BaseSearchIntegrationTest {

  @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
  @Autowired
  private MockMvc mockMvc;

  // disables auditing
  // (necessary -> otherwise the createdBy fields etc. from Project need to be filled)
  // this auditing / security test is done in a separate test
  @MockitoBean
  private AuditingHandler auditingHandler;

  @Autowired
  private CustomSearchService customSearchService;

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

  }

  @Nested
  public class IndexObjects {

    @Test
    public void customIndexCreatesAtLeastOneDocumentInFulltextCore() throws Exception {

      int fulltextCoreDocumentCountInitial = solrClient.countProjectDocuments(
          SolrGamsCores.CUSTOM_SEARCH_CORE.value,
          Set.of(TestProject.PROJECT_ABBR.getValue())
      );
      // at first fulltext core should be empty
      Assertions.assertThat(fulltextCoreDocumentCountInitial).isEqualTo(0);

      mockMvc.perform(
              MockMvcRequestBuilders.post(
                      "/api/v1/integration/projects/{projectAbbr}/objects/customSearch",
                      TestProject.PROJECT_ABBR.getValue()
                  )
                  .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk());

      int fulltextCoreDocumentCount = solrClient.countProjectDocuments(
          SolrGamsCores.CUSTOM_SEARCH_CORE.value,
          Set.of(TestProject.PROJECT_ABBR.getValue())
      );

      Assertions.assertThat(fulltextCoreDocumentCount)
          .isGreaterThan(0);

    }

  }

  @Nested
  public class DeleteIndexedObjects {

    @Test
    public void customDeleteRemovesAllProjectDocumentsFromFulltextCore() throws Exception {

      customSearchService.indexObjects(TestProject.PROJECT_ABBR.getValue());

      // first index some documents
      int fulltextCoreDocumentCountInitial = solrClient.countProjectDocuments(
          SolrGamsCores.CUSTOM_SEARCH_CORE.value,
          Set.of(TestProject.PROJECT_ABBR.getValue())
      );

      Assertions.assertThat(fulltextCoreDocumentCountInitial)
          .isGreaterThan(0);


      // now delete the indexed documents
      mockMvc.perform(
              MockMvcRequestBuilders.delete(
                      "/api/v1/integration/projects/{projectAbbr}/objects/customSearch",
                      TestProject.PROJECT_ABBR.getValue()
                  )
                  .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk());

      int fulltextCoreDocumentCountAfterDelete = solrClient.countProjectDocuments(
          SolrGamsCores.CUSTOM_SEARCH_CORE.value,
          Set.of(TestProject.PROJECT_ABBR.getValue())
      );

      Assertions.assertThat(fulltextCoreDocumentCountAfterDelete)
          .isEqualTo(0);

    }

  }

}
