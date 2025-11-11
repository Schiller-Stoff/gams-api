package org.ddh.gamsapi.application.Integration.PlexusSearch;

import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.ddh.gamsapi.TestUtilities.TestBag;
import org.ddh.gamsapi.TestUtilities.TestProject;
import org.ddh.gamsapi.application.Ingest.Ingest;
import org.ddh.gamsapi.application.Ingest.interfaces.IIngestService;
import org.ddh.gamsapi.application.Ingest.utils.ZipUtils;
import org.ddh.gamsapi.application.Integration.Common.utils.solr.SolrGamsCores;
import org.ddh.gamsapi.application.Integration.SolrIntegrationTest;
import org.ddh.gamsapi.domain.Project.ProjectBuilder;
import org.ddh.gamsapi.domain.Project.interfaces.IProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.auditing.AuditingHandler;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.io.File;
import java.io.IOException;
import java.util.Set;

@Slf4j
public class PlexusSearchServiceIT extends SolrIntegrationTest {

  @Autowired
  private PlexusSearchService plexusSearchService;


  @Autowired
  private IIngestService ingestService;

  @Autowired
  private IProjectRepository projectRepository;

  // disables auditing
  @MockitoBean
  private AuditingHandler auditingHandler;

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
    public void indexCreatesAtLeast1SolrDocument(){

      int initialDocumentsCount = solrClient.countProjectDocuments(
          SolrGamsCores.PLEXUS_SEARCH_CORE.value,
          Set.of(TestProject.PROJECT_ABBR.getValue())
      );

      Assertions.assertThat(initialDocumentsCount).isEqualTo(0);

      // run the indexing
      plexusSearchService.indexObjects(TestProject.PROJECT_ABBR.getValue());
      int finalDocumentsCount = solrClient.countProjectDocuments(
          SolrGamsCores.PLEXUS_SEARCH_CORE.value,
          Set.of(TestProject.PROJECT_ABBR.getValue())
      );
      // assert expected number of documents created
      Assertions.assertThat(finalDocumentsCount).isGreaterThan(0); // expecting some documents to be indexed

    }

    @Test
    public void indexCreatesDocumentsWithExpectedFields(){

      final String TEST_SOLR_DOCUMENT_ID = "test.9124719230";

      // run the indexing
      plexusSearchService.indexObjects(TestProject.PROJECT_ABBR.getValue());

      var solrDocument = solrClient.retrieveSolrDocumentById(
          SolrGamsCores.PLEXUS_SEARCH_CORE.value,
          TEST_SOLR_DOCUMENT_ID
      );

      Assertions.assertThat(solrDocument)
          .isNotNull();

      Assertions.assertThat(solrDocument.getProperty("id"))
          .isEqualTo(TEST_SOLR_DOCUMENT_ID);


    }


  }

}
