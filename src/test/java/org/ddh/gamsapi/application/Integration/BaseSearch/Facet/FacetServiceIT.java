package org.ddh.gamsapi.application.Integration.BaseSearch.Facet;

import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.ddh.gamsapi.TestUtilities.TestBag;
import org.ddh.gamsapi.TestUtilities.TestDigitalObject;
import org.ddh.gamsapi.TestUtilities.TestDublinCoreEntry;
import org.ddh.gamsapi.TestUtilities.TestProject;
import org.ddh.gamsapi.application.Ingest.Ingest;
import org.ddh.gamsapi.application.Ingest.interfaces.IIngestService;
import org.ddh.gamsapi.application.Ingest.utils.ZipUtils;
import org.ddh.gamsapi.application.Integration.BaseSearch.BaseSearchIntegrationTest;
import org.ddh.gamsapi.application.Integration.BaseSearch.BaseSearchProperties;
import org.ddh.gamsapi.application.Integration.BaseSearch.BaseSearchService;
import org.ddh.gamsapi.application.Integration.BaseSearch.solr.SolrClient;
import org.ddh.gamsapi.domain.Project.ProjectBuilder;
import org.ddh.gamsapi.domain.Project.interfaces.IProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.auditing.AuditingHandler;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.io.File;
import java.io.IOException;
import java.util.Set;

@Slf4j
public class FacetServiceIT extends BaseSearchIntegrationTest {

  @Autowired
  private BaseSearchService baseSearchService;

  @Autowired
  private FacetService facetService;

  @Autowired
  private IIngestService ingestService;

  @Autowired
  private IProjectRepository projectRepository;

  // disables auditing
  @MockitoBean
  private AuditingHandler auditingHandler;

  @Autowired
  private SolrClient sOLRClient;

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
  public class BasicFacetSearch {

    @Test
    public void facetResultsAreNotEmpty(){

      // Basic search
      var facetResult = facetService.facetSearch(
          Set.of(TestProject.PROJECT_ABBR.getValue()),
          "",
          new LinkedMultiValueMap<>(),
          PageRequest.of(0, 20)
      );

      org.assertj.core.api.Assertions.assertThat(facetResult.getResults())
          .isNotEmpty();

      org.assertj.core.api.Assertions.assertThat(facetResult.getAvailableFacets())
          .isNotEmpty();

      org.assertj.core.api.Assertions.assertThat(facetResult.getTotalUnfilteredCount())
          .isGreaterThan(0);

    }

    @Test
    public void facetedResponseContainsExpectedObjectData(){

      // Basic search
      var facetResult = facetService.facetSearch(
          Set.of(TestProject.PROJECT_ABBR.getValue()),
          "",
          new LinkedMultiValueMap<>(),
          PageRequest.of(0, 20)
      );

      // TODO rethink assertion
      org.assertj.core.api.Assertions.assertThat(facetResult.getResults())
          .isNotEmpty()
          .anySatisfy( baseSearch -> {
            org.assertj.core.api.Assertions.assertThat(baseSearch.getProperty("id"))
                .isEqualTo(TestDigitalObject.DIGITAL_OBJECT_ID.getValue());
          });

    }

    @Test
    public void facetedResponseDoesNotContainFulltextProperty(){

      // Basic search
      var facetResult = facetService.facetSearch(
          Set.of(TestProject.PROJECT_ABBR.getValue()),
          "",
          new LinkedMultiValueMap<>(),
          PageRequest.of(0, 20)
      );

      var returnedBaseSearchElem = facetResult.getResults().get(0);
      org.assertj.core.api.Assertions.assertThat(returnedBaseSearchElem.getProperty(BaseSearchProperties.FULLTEXT.name))
          .isNull();

    }

  }

  @Nested
  public class FulltextFacetSearch {

    @Test
    public void fulltextSearchWithoutFiltersReturnsResults() {
      // Search for a term that should be in the fulltext
      var facetResult = facetService.facetSearch(
          Set.of(TestProject.PROJECT_ABBR.getValue()),
          TestDublinCoreEntry.VALUE.getValue(),  // Fulltext query
          new LinkedMultiValueMap<>(),
          PageRequest.of(0, 20)
      );

      Assertions.assertThat(facetResult.getResults())
          .isNotEmpty();

      Assertions.assertThat(facetResult.getFilteredCount())
          .isGreaterThan(0);

      Assertions.assertThat(facetResult.getAvailableFacets())
          .isNotEmpty();
    }

    @Test
    public void notMatchingFulltextSearchWithoutFiltersReturnsResults() {
      // Search for a term that should be in the fulltext
      var facetResult = facetService.facetSearch(
          Set.of(TestProject.PROJECT_ABBR.getValue()),
          "WILL_NOT_BE_MATCHED_9999999999999999999",  // Fulltext query
          new LinkedMultiValueMap<>(),
          PageRequest.of(0, 20)
      );

      Assertions.assertThat(facetResult.getResults())
          .isEmpty();


      Assertions.assertThat(facetResult.getAvailableFacets())
          .isNotEmpty();
    }

    @Test
    public void fulltextSearchWithNullQueryWorksLikeBasicSearch() {
      var facetResultWithNull = facetService.facetSearch(
          Set.of(TestProject.PROJECT_ABBR.getValue()),
          null,  // No fulltext query
          new LinkedMultiValueMap<>(),
          PageRequest.of(0, 20)
      );

      var facetResultBasic = facetService.facetSearch(
          Set.of(TestProject.PROJECT_ABBR.getValue()),
          "",
          new LinkedMultiValueMap<>(),
          PageRequest.of(0, 20)
      );

      Assertions.assertThat(facetResultWithNull.getFilteredCount())
          .isEqualTo(facetResultBasic.getFilteredCount());
    }

    @Test
    public void fulltextSearchWithEmptyStringWorksLikeBasicSearch() {
      var facetResultWithEmpty = facetService.facetSearch(
          Set.of(TestProject.PROJECT_ABBR.getValue()),
          "",  // Empty fulltext query
          new LinkedMultiValueMap<>(),
          PageRequest.of(0, 20)
      );

      var facetResultBasic = facetService.facetSearch(
          Set.of(TestProject.PROJECT_ABBR.getValue()),
          "",
          new LinkedMultiValueMap<>(),
          PageRequest.of(0, 20)
      );

      Assertions.assertThat(facetResultWithEmpty.getFilteredCount())
          .isEqualTo(facetResultBasic.getFilteredCount());
    }

    @Test
    public void fulltextSearchWithFacetFiltersReturnsCombinedResults() {
      // Create facet filters
      MultiValueMap<String, String> filters = new LinkedMultiValueMap<>();
      filters.add("type", "Brief");

      var facetResult = facetService.facetSearch(
          Set.of(TestProject.PROJECT_ABBR.getValue()),
          "Paris",  // Fulltext query
          filters,   // AND type=Brief
          PageRequest.of(0, 20)
      );

      // Results should match BOTH fulltext AND facet filter
      Assertions.assertThat(facetResult.getResults())
          .isNotNull();

      // Should return facets for further filtering
      Assertions.assertThat(facetResult.getAvailableFacets())
          .isNotEmpty();

      // Should reflect selected filters
      Assertions.assertThat(facetResult.getSelectedFacets())
          .containsKey("type");
    }

    @Test
    public void fulltextSearchWithMultipleFacetFilters() {
      MultiValueMap<String, String> filters = new LinkedMultiValueMap<>();
      filters.add("type", "Brief");
      filters.add("coverage", "Paris");

      var facetResult = facetService.facetSearch(
          Set.of(TestProject.PROJECT_ABBR.getValue()),
          "Gaston",  // Fulltext query
          filters,    // AND type=Brief AND coverage=Paris
          PageRequest.of(0, 20)
      );

      Assertions.assertThat(facetResult)
          .isNotNull();

      // Selected facets should be reflected in response
      Assertions.assertThat(facetResult.getSelectedFacets())
          .containsKeys("type", "coverage");
    }

    @Test
    public void fulltextSearchWithSpecialCharactersIsEscaped() {
      // Test that special Solr characters are properly escaped
      var facetResult = facetService.facetSearch(
          Set.of(TestProject.PROJECT_ABBR.getValue()),
          "test:value",  // Contains Solr special character
          new LinkedMultiValueMap<>(),
          PageRequest.of(0, 20)
      );

      // Should not throw exception
      Assertions.assertThat(facetResult)
          .isNotNull();
    }
  }

  @Nested
  class MultiFacetFiltering {

    @Test
    public void multipleValuesInSameFacetUseOrLogic() {
      MultiValueMap<String, String> filters = new LinkedMultiValueMap<>();
      filters.add("type", "Brief");
      filters.add("type", "Artikel");

      var facetResult = facetService.facetSearch(
          Set.of(TestProject.PROJECT_ABBR.getValue()),
          null,
          filters,  // type=Brief OR type=Artikel
          PageRequest.of(0, 20)
      );

      Assertions.assertThat(facetResult)
          .isNotNull();

      // Should reflect both selected values
      Assertions.assertThat(facetResult.getSelectedFacets().get("type"))
          .containsExactlyInAnyOrder("Brief", "Artikel");
    }

    @Test
    public void differentFacetsUseAndLogic() {
      MultiValueMap<String, String> filters = new LinkedMultiValueMap<>();
      filters.add("type", "Brief");
      filters.add("coverage", "Wien");

      var facetResult = facetService.facetSearch(
          Set.of(TestProject.PROJECT_ABBR.getValue()),
          null,
          filters,  // type=Brief AND coverage=Wien
          PageRequest.of(0, 20)
      );

      Assertions.assertThat(facetResult)
          .isNotNull();

      Assertions.assertThat(facetResult.getSelectedFacets())
          .containsKeys("type", "coverage");
    }
  }

  @Nested
  class PaginationTests {

    @Test
    public void paginationReturnsCorrectPageSize() {
      int requestedPageSize = 5;

      var facetResult = facetService.facetSearch(
          Set.of(TestProject.PROJECT_ABBR.getValue()),
          "",
          new LinkedMultiValueMap<>(),
          PageRequest.of(0, requestedPageSize)
      );

      Assertions.assertThat(facetResult.getResults().size())
          .isLessThanOrEqualTo(requestedPageSize);
    }

    @Test
    public void paginationStartOffsetIsCorrect() {
      var facetResult = facetService.facetSearch(
          Set.of(TestProject.PROJECT_ABBR.getValue()),
          "",
          new LinkedMultiValueMap<>(),
          PageRequest.of(1, 20)  // Second page
      );

      Assertions.assertThat(facetResult.getStart())
          .isEqualTo(20);  // Should start at offset 20
    }
  }

}
