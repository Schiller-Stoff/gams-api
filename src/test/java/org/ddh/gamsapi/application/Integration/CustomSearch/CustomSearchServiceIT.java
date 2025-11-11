package org.ddh.gamsapi.application.Integration.CustomSearch;

import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.ddh.gamsapi.TestUtilities.TestBag;
import org.ddh.gamsapi.TestUtilities.TestDigitalObject;
import org.ddh.gamsapi.TestUtilities.TestProject;
import org.ddh.gamsapi.application.Ingest.Ingest;
import org.ddh.gamsapi.application.Ingest.interfaces.IIngestService;
import org.ddh.gamsapi.application.Ingest.utils.ZipUtils;
import org.ddh.gamsapi.application.Integration.SolrIntegrationTest;
import org.ddh.gamsapi.application.Integration.Common.utils.solr.SolrGamsCores;
import org.ddh.gamsapi.domain.Project.ProjectBuilder;
import org.ddh.gamsapi.domain.Project.interfaces.IProjectRepository;
import org.ddh.gamsapi.infrastructure.System.configproperties.GAMSDockerDNS;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.auditing.AuditingHandler;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

@Slf4j
public class CustomSearchServiceIT extends SolrIntegrationTest {

  @Autowired
  private CustomSearchService customSearchService;

  @Autowired
  private IIngestService ingestService;

  @Autowired
  private IProjectRepository projectRepository;

  @Autowired
  private GAMSDockerDNS gamsDockerDNS;

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
  public class Indexing {

    @Test
    public void customFulltextIndexingIndexesExpectedData(){

      int initialDocumentsCount = solrClient.countProjectDocuments(
          SolrGamsCores.CUSTOM_SEARCH_CORE.value,
          Set.of(TestProject.PROJECT_ABBR.getValue())
      );

      Assertions.assertThat(initialDocumentsCount).isEqualTo(0);

      customSearchService.indexObjects(TestProject.PROJECT_ABBR.getValue());

      String response = solrClient.retrieveSolrDocumentByProperty(
          SolrGamsCores.CUSTOM_SEARCH_CORE.value, "objectId", TestDigitalObject.DIGITAL_OBJECT_ID.getValue()
      );

      int solrDocumentCount = solrClient.countProjectDocuments(SolrGamsCores.CUSTOM_SEARCH_CORE.value, Set.of(TestProject.PROJECT_ABBR.getValue()));

      org.assertj.core.api.Assertions.assertThat(solrDocumentCount)
          .isGreaterThan(0);

      org.assertj.core.api.Assertions.assertThat(response)
          .isNotNull()
          .contains("\"objectId\":\""+ TestDigitalObject.DIGITAL_OBJECT_ID.getValue());

    }



  }

  @Nested
  public class IndexObject {

    @Test
    public void indexSingleObjectIndexesExpectedData(){

      int initialDocumentsCount = solrClient.countProjectDocuments(
          SolrGamsCores.CUSTOM_SEARCH_CORE.value,
          Set.of(TestProject.PROJECT_ABBR.getValue())
      );

      Assertions.assertThat(initialDocumentsCount).isEqualTo(0);

      customSearchService.indexObject(
          TestProject.PROJECT_ABBR.getValue(),
          TestDigitalObject.DIGITAL_OBJECT_ID.getValue()
      );

      String response = solrClient.retrieveSolrDocumentByProperty(
          SolrGamsCores.CUSTOM_SEARCH_CORE.value, "objectId", TestDigitalObject.DIGITAL_OBJECT_ID.getValue()
      );

      int solrDocumentCount = solrClient.countProjectDocuments(SolrGamsCores.CUSTOM_SEARCH_CORE.value, Set.of(TestProject.PROJECT_ABBR.getValue()));

      org.assertj.core.api.Assertions.assertThat(solrDocumentCount)
          .isEqualTo(1);

      org.assertj.core.api.Assertions.assertThat(response)
          .isNotNull()
          .contains("\"objectId\":\""+ TestDigitalObject.DIGITAL_OBJECT_ID.getValue());

    }


  }

  @Nested
  public class Search {

    @Test
    public void customFulltextSearchReturnsExpectedResults(){

      customSearchService.indexObjects(TestProject.PROJECT_ABBR.getValue());

      final String FULLTEXT_QUERY = "";

      var responseDto = customSearchService.search(
          FULLTEXT_QUERY,
          Set.of(TestProject.PROJECT_ABBR.getValue()),
          List.of(),
          "",
          "",
          PageRequest.of(0,100)
      );

      org.assertj.core.api.Assertions.assertThat(responseDto)
          .isNotNull();

      Assertions.assertThat(responseDto.getResults().getContent())
          .hasSize(1)
          .allMatch(solrDocument -> {
            String objectId = (String) solrDocument.getProperty("objectId");
            return objectId != null && objectId.equals(TestDigitalObject.DIGITAL_OBJECT_ID.getValue());
          })
      ;

    }

    @Test
    void search_withDateRange_returnsOnlyMatchingDocuments() {
      // Index test data with various date ranges
      customSearchService.indexObjects(TestProject.PROJECT_ABBR.getValue());

      var response = customSearchService.search(
          "",
          Set.of(TestProject.PROJECT_ABBR.getValue()),
          List.of(),
          "2023-06-01T00:00:00Z",  // Only 2023 H2 events
          "2023-12-31T23:59:59Z",
          PageRequest.of(0, 100)
      );

      Assertions.assertThat(response.getResults().getContent())
          .allMatch(doc -> {
            // Verify dates are within range
            String startDate = (String) doc.getProperty("entityStartDate");
            return startDate != null && startDate.compareTo("2023-06-01") >= 0;
          });
    }

  }

  @Nested
  public class DeleteIndexedObject {

    @Test
    public void deleteIndexedObjectRemovesExpectedData(){

      customSearchService.indexObject(
          TestProject.PROJECT_ABBR.getValue(),
          TestDigitalObject.DIGITAL_OBJECT_ID.getValue()
      );

      int solrDocumentCount = solrClient.countProjectDocuments(SolrGamsCores.CUSTOM_SEARCH_CORE.value, Set.of(TestProject.PROJECT_ABBR.getValue()));

      org.assertj.core.api.Assertions.assertThat(solrDocumentCount)
          .isEqualTo(1);

      customSearchService.deleteIndexedObject(
          TestProject.PROJECT_ABBR.getValue(),
          TestDigitalObject.DIGITAL_OBJECT_ID.getValue()
      );

      solrDocumentCount = solrClient.countProjectDocuments(SolrGamsCores.CUSTOM_SEARCH_CORE.value, Set.of(TestProject.PROJECT_ABBR.getValue()));

      org.assertj.core.api.Assertions.assertThat(solrDocumentCount)
          .isEqualTo(0);

    }


  }

}
