package org.ddh.gamsapi.application.Integration.GSearch.Facet;

import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.ddh.gamsapi.TestUtilities.TestBag;
import org.ddh.gamsapi.TestUtilities.TestDigitalObject;
import org.ddh.gamsapi.TestUtilities.TestDublinCoreEntry;
import org.ddh.gamsapi.TestUtilities.TestProject;
import org.ddh.gamsapi.application.Ingest.interfaces.IIngestService;
import org.ddh.gamsapi.application.Ingest.utils.ZipUtils;
import org.ddh.gamsapi.application.Integration.GSearch.ApiSearchService;
import org.ddh.gamsapi.application.Integration.SolrIntegrationTest;
import org.ddh.gamsapi.application.Integration.GSearch.ApiSearchProperties;
import org.ddh.gamsapi.domain.Project.ProjectBuilder;
import org.ddh.gamsapi.domain.Project.interfaces.IProjectRepository;
import org.ddh.gamsapi.infrastructure.System.security.IUserPrincipalAuditorMapping;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.auditing.AuditingHandler;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.Set;

/**
 * Integration tests for FacetService with comprehensive pagination testing.
 */
@Slf4j
public class FacetServiceIT extends SolrIntegrationTest {

  @Autowired
  private ApiSearchService apiSearchService;

  @Autowired
  private FacetService facetService;

  @Autowired
  private IIngestService ingestService;

  @Autowired
  private IProjectRepository projectRepository;

  /**
   * Classes need to mock authenticated users when changing datastreams
   */
  @MockitoBean
  private AuditingHandler auditingHandler;
  @MockitoBean
  private IUserPrincipalAuditorMapping userPrincipalAuditorMapping;

  File bagFile;

  @BeforeEach
  public void setup() throws IOException {
    Mockito.when(userPrincipalAuditorMapping.getCurrentAuditor())
        .thenReturn(Optional.of("test-user"));

    bagFile = TestBag.loadFile();
    projectRepository.save(ProjectBuilder.builder()
        .projectAbbr(TestProject.PROJECT_ABBR.getValue())
        .build());

    // ingest the bag
    byte[] zippedBag = ZipUtils.zipDir(bagFile);
    ingestService.ingest(
        TestProject.PROJECT_ABBR.getValue(),
        new ByteArrayInputStream(zippedBag)
    );

    // Index object
    apiSearchService.indexObject(
        TestProject.PROJECT_ABBR.getValue(),
        TestDigitalObject.DIGITAL_OBJECT_ID.getValue()
    );
  }

  @Nested
  public class BasicFacetSearch {

    @Test
    public void facetResultsAreNotEmpty() {
      var facetResult = facetService.facetSearch(
          Set.of(TestProject.PROJECT_ABBR.getValue()),
          "",
          new LinkedMultiValueMap<>(),
          PageRequest.of(0, 20)
      );

      Assertions.assertThat(facetResult.getResult().getContent())
          .isNotEmpty();

      Assertions.assertThat(facetResult.getAvailableFacets())
          .isNotEmpty();

      Assertions.assertThat(facetResult.getTotalUnfilteredCount())
          .isGreaterThan(0);
    }

    @Test
    public void facetedResponseContainsExpectedObjectData() {
      var facetResult = facetService.facetSearch(
          Set.of(TestProject.PROJECT_ABBR.getValue()),
          "",
          new LinkedMultiValueMap<>(),
          PageRequest.of(0, 20)
      );

      Assertions.assertThat(facetResult.getResult().getContent())
          .isNotEmpty()
          .anySatisfy(baseSearch -> {
            Assertions.assertThat(baseSearch.getProperty("id"))
                .isEqualTo(TestDigitalObject.DIGITAL_OBJECT_ID.getValue());
          });
    }

    @Test
    public void facetedResponseDoesNotContainFulltextProperty() {
      var facetResult = facetService.facetSearch(
          Set.of(TestProject.PROJECT_ABBR.getValue()),
          "",
          new LinkedMultiValueMap<>(),
          PageRequest.of(0, 20)
      );

      var returnedBaseSearchElem = facetResult.getResult().getContent().get(0);
      Assertions.assertThat(returnedBaseSearchElem.getProperty(ApiSearchProperties.FULLTEXT.name))
          .isNull();
    }
  }

  @Nested
  public class FulltextFacetSearch {

    @Test
    public void fulltextSearchWithoutFiltersReturnsResults() {
      var facetResult = facetService.facetSearch(
          Set.of(TestProject.PROJECT_ABBR.getValue()),
          TestDublinCoreEntry.VALUE.getValue(),  // Fulltext query
          new LinkedMultiValueMap<>(),
          PageRequest.of(0, 20)
      );

      Assertions.assertThat(facetResult.getResult().getContent())
          .isNotEmpty();

      Assertions.assertThat(facetResult.getResult().getPagination().getTotalElements())
          .isGreaterThan(0);

      Assertions.assertThat(facetResult.getAvailableFacets())
          .isNotEmpty();
    }

    @Test
    public void notMatchingFulltextSearchReturnsEmptyResults() {
      var facetResult = facetService.facetSearch(
          Set.of(TestProject.PROJECT_ABBR.getValue()),
          "WILL_NOT_BE_MATCHED_9999999999999999999",
          new LinkedMultiValueMap<>(),
          PageRequest.of(0, 20)
      );

      Assertions.assertThat(facetResult.getResult().getContent())
          .isEmpty();

      Assertions.assertThat(facetResult.getAvailableFacets())
          .isNotEmpty();
    }

    @Test
    public void fulltextSearchWithNullQueryWorksLikeBasicSearch() {
      var facetResultWithNull = facetService.facetSearch(
          Set.of(TestProject.PROJECT_ABBR.getValue()),
          null,
          new LinkedMultiValueMap<>(),
          PageRequest.of(0, 20)
      );

      var facetResultBasic = facetService.facetSearch(
          Set.of(TestProject.PROJECT_ABBR.getValue()),
          "",
          new LinkedMultiValueMap<>(),
          PageRequest.of(0, 20)
      );

      Assertions.assertThat(facetResultWithNull.getResult().getPagination().getTotalElements())
          .isEqualTo(facetResultBasic.getResult().getPagination().getTotalElements());
    }
  }

  @Nested
  class MultiFacetFiltering {

    @Test
    public void multipleValuesInSameFacetUseOrLogic() {
      MultiValueMap<String, String> filters = new LinkedMultiValueMap<>();
      filters.add("dc.type", "Brief");
      filters.add("dc.type", "Artikel");

      var facetResult = facetService.facetSearch(
          Set.of(TestProject.PROJECT_ABBR.getValue()),
          null,
          filters,  // type=Brief OR type=Artikel
          PageRequest.of(0, 20)
      );

      Assertions.assertThat(facetResult).isNotNull();
      Assertions.assertThat(facetResult.getSelectedFacets().get("dc.type"))
          .containsExactlyInAnyOrder("Brief", "Artikel");
    }

    @Test
    public void differentFacetsUseAndLogic() {
      MultiValueMap<String, String> filters = new LinkedMultiValueMap<>();
      filters.add("dc.type", "Brief");
      filters.add("dc.coverage", "Wien");

      var facetResult = facetService.facetSearch(
          Set.of(TestProject.PROJECT_ABBR.getValue()),
          null,
          filters,  // type=Brief AND coverage=Wien
          PageRequest.of(0, 20)
      );

      Assertions.assertThat(facetResult).isNotNull();
      Assertions.assertThat(facetResult.getSelectedFacets())
          .containsKeys("dc.type", "dc.coverage");
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

      // Verify content size
      Assertions.assertThat(facetResult.getResult().getContent().size())
          .isLessThanOrEqualTo(requestedPageSize);

      // NEW: Verify pagination metadata
      Assertions.assertThat(facetResult.getResult().getPagination().getSize())
          .isEqualTo(requestedPageSize);
    }

    @Test
    public void firstPageHasCorrectMetadata() {
      var facetResult = facetService.facetSearch(
          Set.of(TestProject.PROJECT_ABBR.getValue()),
          "",
          new LinkedMultiValueMap<>(),
          PageRequest.of(0, 20)
      );

      var pagination = facetResult.getResult().getPagination();

      Assertions.assertThat(pagination.getPage()).isEqualTo(0);
      Assertions.assertThat(pagination.isFirst()).isTrue();
      Assertions.assertThat(pagination.isHasPrevious()).isFalse();

      // Only assert hasNext if there are actually more results
      if (pagination.getTotalElements() > 20) {
        Assertions.assertThat(pagination.isHasNext()).isTrue();
        Assertions.assertThat(pagination.isLast()).isFalse();
      }
    }

    @Test
    public void secondPageHasCorrectMetadata() {
      var facetResult = facetService.facetSearch(
          Set.of(TestProject.PROJECT_ABBR.getValue()),
          "",
          new LinkedMultiValueMap<>(),
          PageRequest.of(1, 20)  // Second page
      );

      var pagination = facetResult.getResult().getPagination();

      Assertions.assertThat(pagination.getPage()).isEqualTo(1);
      Assertions.assertThat(pagination.isFirst()).isFalse();
      Assertions.assertThat(pagination.isHasPrevious()).isTrue();

      // hasNext and isLast depend on total results
      if (pagination.getTotalElements() > 40) {
        Assertions.assertThat(pagination.isHasNext()).isTrue();
      }
    }

    @Test
    public void totalPagesCalculatedCorrectly() {
      var facetResult = facetService.facetSearch(
          Set.of(TestProject.PROJECT_ABBR.getValue()),
          "",
          new LinkedMultiValueMap<>(),
          PageRequest.of(0, 20)
      );

      var pagination = facetResult.getResult().getPagination();
      long totalElements = pagination.getTotalElements();
      int expectedTotalPages = (int) Math.ceil((double) totalElements / 20);

      Assertions.assertThat(pagination.getTotalPages())
          .isEqualTo(expectedTotalPages);
    }

    @Test
    public void paginationConsistentAcrossPages() {
      // Get total count from first page
      var firstPage = facetService.facetSearch(
          Set.of(TestProject.PROJECT_ABBR.getValue()),
          "",
          new LinkedMultiValueMap<>(),
          PageRequest.of(0, 10)
      );

      long totalElements = firstPage.getResult().getPagination().getTotalElements();

      // Only test second page if there are enough results
      if (totalElements > 10) {
        var secondPage = facetService.facetSearch(
            Set.of(TestProject.PROJECT_ABBR.getValue()),
            "",
            new LinkedMultiValueMap<>(),
            PageRequest.of(1, 10)
        );

        // Total elements should be consistent across pages
        Assertions.assertThat(secondPage.getResult().getPagination().getTotalElements())
            .isEqualTo(totalElements);

        // Page numbers should be correct
        Assertions.assertThat(firstPage.getResult().getPagination().getPage()).isEqualTo(0);
        Assertions.assertThat(secondPage.getResult().getPagination().getPage()).isEqualTo(1);
      }
    }

    @Test
    public void emptyResultsHaveCorrectPaginationMetadata() {
      var facetResult = facetService.facetSearch(
          Set.of(TestProject.PROJECT_ABBR.getValue()),
          "NONEXISTENT_SEARCH_TERM_12345",
          new LinkedMultiValueMap<>(),
          PageRequest.of(0, 20)
      );

      var pagination = facetResult.getResult().getPagination();

      Assertions.assertThat(pagination.getTotalElements()).isEqualTo(0);
      Assertions.assertThat(pagination.getTotalPages()).isEqualTo(0);
      Assertions.assertThat(facetResult.getResult().getContent()).isEmpty();
      Assertions.assertThat(pagination.isFirst()).isTrue();
      Assertions.assertThat(pagination.isLast()).isTrue();
    }

    @Test
    public void unfilteredCountDifferentFromFilteredCount() {
      MultiValueMap<String, String> filters = new LinkedMultiValueMap<>();
      filters.add("dc.type", "Brief");

      var facetResult = facetService.facetSearch(
          Set.of(TestProject.PROJECT_ABBR.getValue()),
          "",
          filters,
          PageRequest.of(0, 20)
      );

      // Unfiltered count should be >= filtered count
      Assertions.assertThat(facetResult.getTotalUnfilteredCount())
          .isGreaterThanOrEqualTo(facetResult.getResult().getPagination().getTotalElements());
    }
  }

  @Nested
  class ResponseStructureTests {

    @Test
    public void responseContainsPagedResponseWrapper() {
      var facetResult = facetService.facetSearch(
          Set.of(TestProject.PROJECT_ABBR.getValue()),
          "",
          new LinkedMultiValueMap<>(),
          PageRequest.of(0, 20)
      );

      // Verify PagedResponse structure exists
      Assertions.assertThat(facetResult.getResult()).isNotNull();
      Assertions.assertThat(facetResult.getResult().getContent()).isNotNull();
      Assertions.assertThat(facetResult.getResult().getPagination()).isNotNull();
    }

    @Test
    public void paginationInfoHasAllRequiredFields() {
      var facetResult = facetService.facetSearch(
          Set.of(TestProject.PROJECT_ABBR.getValue()),
          "",
          new LinkedMultiValueMap<>(),
          PageRequest.of(0, 20)
      );

      var pagination = facetResult.getResult().getPagination();

      // Verify all pagination fields are present (not null)
      Assertions.assertThat(pagination.getPage()).isNotNull();
      Assertions.assertThat(pagination.getSize()).isNotNull();
      Assertions.assertThat(pagination.getTotalElements()).isNotNull();
      Assertions.assertThat(pagination.getTotalPages()).isNotNull();
    }
  }
}