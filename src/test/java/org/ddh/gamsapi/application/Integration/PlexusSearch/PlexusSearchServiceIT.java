package org.ddh.gamsapi.application.Integration.PlexusSearch;

import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.ddh.gamsapi.TestUtilities.TestBag;
import org.ddh.gamsapi.TestUtilities.TestDigitalObject;
import org.ddh.gamsapi.TestUtilities.TestProject;
import org.ddh.gamsapi.application.Ingest.Ingest;
import org.ddh.gamsapi.application.Ingest.interfaces.IIngestService;
import org.ddh.gamsapi.application.Ingest.utils.ZipUtils;
import org.ddh.gamsapi.application.Integration.Common.utils.solr.SolrDocument;
import org.ddh.gamsapi.application.Integration.Common.utils.solr.SolrGamsCores;
import org.ddh.gamsapi.application.Integration.PlexusSearch.dto.PlexusSearchQueryRequestDto;
import org.ddh.gamsapi.application.Integration.PlexusSearch.dto.PlexusSearchResponseDto;
import org.ddh.gamsapi.application.Integration.PlexusSearch.exceptions.PlexusSearchForbiddenQueryException;
import org.ddh.gamsapi.application.Integration.SolrIntegrationTest;
import org.ddh.gamsapi.domain.Project.ProjectBuilder;
import org.ddh.gamsapi.domain.Project.interfaces.IProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.auditing.AuditingHandler;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.io.ByteArrayInputStream;
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
    ingestService.ingest(
        TestProject.PROJECT_ABBR.getValue(),
        new ByteArrayInputStream(zippedBag)
    );
  }

  @Nested
  public class IndexObjects {

    @Test
    public void indexCreatesAtLeast1SolrDocument(){

      int initialDocumentsCount = solrClient.countDocumentsByPropertyValues(
          SolrGamsCores.PLEXUS_SEARCH_CORE.value,
          PlexusSearchProperties.ENTITY_PROJECT_ABBR.name,
          Set.of(TestProject.PROJECT_ABBR.getValue())
      );

      Assertions.assertThat(initialDocumentsCount).isEqualTo(0);

      // run the indexing
      plexusSearchService.indexObjects(TestProject.PROJECT_ABBR.getValue());
      int finalDocumentsCount = solrClient.countDocumentsByPropertyValues(
          SolrGamsCores.PLEXUS_SEARCH_CORE.value,
          PlexusSearchProperties.ENTITY_PROJECT_ABBR.name,
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

  @Nested
  public class IndexObject {

    @Test
    public void indexCreatesASolrDocumentForGivenObjectId() {

      final String TEST_SOLR_DOCUMENT_ID = "test.9124719230";

      // run the indexing
      plexusSearchService.indexObject(
          TestProject.PROJECT_ABBR.getValue(),
          TestDigitalObject.DIGITAL_OBJECT_ID.getValue()
      );

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

  @Nested
  public class DeleteIndexedObjects {

    @Test
    public void deleteRemovesAllProjectDocuments(){

      final String TEST_SOLR_DOCUMENT_ID = "test.1111111";

      // first fill data of plexus search core
      SolrDocument testSolrDocument = new SolrDocument();
      testSolrDocument.addProperty(PlexusSearchProperties.ENTITY_ID.name, TEST_SOLR_DOCUMENT_ID);
      testSolrDocument.addProperty(PlexusSearchProperties.ENTITY_PROJECT_ABBR.name, TestProject.PROJECT_ABBR.getValue());
      testSolrDocument.addProperty(PlexusSearchProperties.ENTITY_OBJECT_ID.name, TestDigitalObject.DIGITAL_OBJECT_ID.getValue());
      // posting data to solr
      solrClient.post(SolrGamsCores.PLEXUS_SEARCH_CORE.value, testSolrDocument);

      Assertions.assertThat(
          solrClient.checkCoreIsEmpty(SolrGamsCores.PLEXUS_SEARCH_CORE.value)
      ).isFalse();

      int documentsCountAfterIndexing = solrClient.countDocumentsByPropertyValues(
          SolrGamsCores.PLEXUS_SEARCH_CORE.value,
          PlexusSearchProperties.ENTITY_PROJECT_ABBR.name,
          Set.of()
      );

      Assertions.assertThat(documentsCountAfterIndexing).isGreaterThan(0); // expecting some documents to be indexed

      // run the deletion
      plexusSearchService.deleteIndexedObjects(TestProject.PROJECT_ABBR.getValue());

      int documentsCountAfterDeletion = solrClient.countDocumentsByPropertyValues(
          SolrGamsCores.PLEXUS_SEARCH_CORE.value,
          PlexusSearchProperties.ENTITY_PROJECT_ABBR.name,
          Set.of(TestProject.PROJECT_ABBR.getValue())
      );

      Assertions.assertThat(documentsCountAfterDeletion).isEqualTo(0); // expecting all documents to be deleted

    }

  }

  @Nested
  public class DeleteIndexedObject {

    @Test
    public void deleteRemovesASolrDocumentForGivenObjectId(){

      final String TEST_SOLR_DOCUMENT_ID = String.format(
          "%s.foobar", TestProject.PROJECT_ABBR.getValue()
      );

      // first index the object
      SolrDocument testSolrDocument = new SolrDocument();
      testSolrDocument.addProperty(PlexusSearchProperties.ENTITY_ID.name, TEST_SOLR_DOCUMENT_ID);
      testSolrDocument.addProperty(PlexusSearchProperties.ENTITY_PROJECT_ABBR.name, TestProject.PROJECT_ABBR.getValue());
      testSolrDocument.addProperty(PlexusSearchProperties.ENTITY_OBJECT_ID.name, TestDigitalObject.DIGITAL_OBJECT_ID.getValue());
      // posting data to solr
      solrClient.post(SolrGamsCores.PLEXUS_SEARCH_CORE.value, testSolrDocument);

      // run the deletion
      plexusSearchService.deleteIndexedObject(
          TestProject.PROJECT_ABBR.getValue(),
          TestDigitalObject.DIGITAL_OBJECT_ID.getValue()
      );

      // count of documents in core is now zero
      var documentsCount = solrClient.countDocumentsByPropertyValues(
          SolrGamsCores.PLEXUS_SEARCH_CORE.value,
          PlexusSearchProperties.ENTITY_PROJECT_ABBR.name,
          Set.of()
      );

      Assertions.assertThat(documentsCount).isEqualTo(0);

    }

  }

  @Nested
  public class Search {

    @BeforeEach
    public void setup(){
      plexusSearchService.indexObjects(TestProject.PROJECT_ABBR.getValue());
    }

    @Test
    public void searchFindAllShouldThrowValidationException(){

      final String FIND_ALL_QUERY = "*:*";

      var plexusSearchQuery = PlexusSearchQueryRequestDto.builder()
          .query(FIND_ALL_QUERY)
          .build();

      Assertions.assertThatThrownBy(
          () ->  plexusSearchService.search(
              TestProject.PROJECT_ABBR.getValue(),
              plexusSearchQuery),
"Match everything query should be forbidden / throw"
      ).isInstanceOf(PlexusSearchForbiddenQueryException.class);

    }

    @Nested
    public class SimpleExactMatchSearch {
      PlexusSearchResponseDto plexusSearchResponseDto;
      @BeforeEach
      public void setup(){
        final String SIMPLE_QUERY = String.format("%s:%s", PlexusSearchProperties.ENTITY_OBJECT_ID.name,
            TestDigitalObject.DIGITAL_OBJECT_ID.getValue()) ;

        var plexusSearchQuery = PlexusSearchQueryRequestDto.builder()
            .query(SIMPLE_QUERY)
            .build();

        plexusSearchResponseDto = plexusSearchService.search(
            TestProject.PROJECT_ABBR.getValue(),
            plexusSearchQuery
        );
      }

      @Test
      public void returnsAResult(){
        Assertions.assertThat(plexusSearchResponseDto.getTotalCount())
            .isEqualTo(1);
      }

      @Test
      public void responseContainsExecutionTime(){
        Assertions.assertThat(plexusSearchResponseDto.getExecutionTimeMs())
            .isNotNull();

      }

      @Test
      public void responseContainsQueryHints(){
        Assertions.assertThat(plexusSearchResponseDto.getHints())
            .isNotNull();
      }

    }




  }

}
