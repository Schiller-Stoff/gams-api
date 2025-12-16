package org.ddh.gamsapi.application.Integration.GSearch.Facet;

import org.assertj.core.api.Assertions;
import org.ddh.gamsapi.UnitTest;
import org.ddh.gamsapi.application.Integration.Common.utils.solr.SolrDocument;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Comprehensive unit tests for FacetResponseDTO with focus on pagination metadata.
 */
public class FacetResponseDtoTest extends UnitTest {

  @Nested
  class BasicMapping {

    @Test
    public void fromProducesNotNullObjectWithNoNullProperties() {
      FacetSolrResponse solrResponse = createSolrResponse(1, 0, 1);
      Pageable pageable = PageRequest.of(0, 20);

      var mapped = FacetResponseDTO.from(
          solrResponse,
          new LinkedMultiValueMap<>(),
          1,
          pageable
      );

      Assertions.assertThat(mapped)
          .isNotNull()
          .hasNoNullFieldsOrProperties();
    }

    @Test
    public void fromMapsCorrectNumberOfResults() {
      int numDocuments = 5;
      FacetSolrResponse solrResponse = createSolrResponse(numDocuments, 0, numDocuments);
      Pageable pageable = PageRequest.of(0, 20);

      var mapped = FacetResponseDTO.from(
          solrResponse,
          new LinkedMultiValueMap<>(),
          100,
          pageable
      );

      Assertions.assertThat(mapped.getResult().getContent())
          .hasSize(numDocuments);
    }

    @Test
    public void fromPreservesTotalUnfilteredCount() {
      int TOTAL_UNFILTERED = 1000;
      FacetSolrResponse solrResponse = createSolrResponse(10, 0, 50);
      Pageable pageable = PageRequest.of(0, 20);

      var mapped = FacetResponseDTO.from(
          solrResponse,
          new LinkedMultiValueMap<>(),
          TOTAL_UNFILTERED,
          pageable
      );

      Assertions.assertThat(mapped.getTotalUnfilteredCount())
          .isEqualTo(TOTAL_UNFILTERED);
    }

    @Test
    public void fromConvertsSelectedFacetsCorrectly() {
      MultiValueMap<String, String> selectedFacets = new LinkedMultiValueMap<>();
      selectedFacets.add("type", "Brief");
      selectedFacets.add("type", "Artikel");
      selectedFacets.add("coverage", "Wien");

      FacetSolrResponse solrResponse = createSolrResponse(5, 0, 5);
      Pageable pageable = PageRequest.of(0, 20);

      var mapped = FacetResponseDTO.from(
          solrResponse,
          selectedFacets,
          100,
          pageable
      );

      Assertions.assertThat(mapped.getSelectedFacets())
          .containsKeys("type", "coverage")
          .containsEntry("type", List.of("Brief", "Artikel"))
          .containsEntry("coverage", List.of("Wien"));
    }
  }

  @Nested
  class PaginationMetadata {

    @Test
    public void firstPageHasCorrectPaginationMetadata() {
      // Page 0, 20 items per page, 156 total results
      FacetSolrResponse solrResponse = createSolrResponse(20, 0, 156);
      Pageable pageable = PageRequest.of(0, 20);

      var result = FacetResponseDTO.from(
          solrResponse,
          new LinkedMultiValueMap<>(),
          1000,
          pageable
      );

      var pagination = result.getResult().getPagination();

      Assertions.assertThat(pagination.getPage()).isEqualTo(0);
      Assertions.assertThat(pagination.getSize()).isEqualTo(20);
      Assertions.assertThat(pagination.getTotalElements()).isEqualTo(156);
      Assertions.assertThat(pagination.getTotalPages()).isEqualTo(8);  // ceil(156/20)
      Assertions.assertThat(pagination.isHasNext()).isTrue();
      Assertions.assertThat(pagination.isHasPrevious()).isFalse();
      Assertions.assertThat(pagination.isFirst()).isTrue();
      Assertions.assertThat(pagination.isLast()).isFalse();
    }

    @Test
    public void middlePageHasCorrectPaginationMetadata() {
      // Page 2 (third page), 20 items per page, 156 total results
      FacetSolrResponse solrResponse = createSolrResponse(20, 40, 156);
      Pageable pageable = PageRequest.of(2, 20);

      var result = FacetResponseDTO.from(
          solrResponse,
          new LinkedMultiValueMap<>(),
          1000,
          pageable
      );

      var pagination = result.getResult().getPagination();

      Assertions.assertThat(pagination.getPage()).isEqualTo(2);
      Assertions.assertThat(pagination.getSize()).isEqualTo(20);
      Assertions.assertThat(pagination.getTotalElements()).isEqualTo(156);
      Assertions.assertThat(pagination.getTotalPages()).isEqualTo(8);
      Assertions.assertThat(pagination.isHasNext()).isTrue();
      Assertions.assertThat(pagination.isHasPrevious()).isTrue();
      Assertions.assertThat(pagination.isFirst()).isFalse();
      Assertions.assertThat(pagination.isLast()).isFalse();
    }

    @Test
    public void lastPageHasCorrectPaginationMetadata() {
      // Page 7 (last page), 20 items per page, 156 total results (16 items on last page)
      FacetSolrResponse solrResponse = createSolrResponse(16, 140, 156);
      Pageable pageable = PageRequest.of(7, 20);

      var result = FacetResponseDTO.from(
          solrResponse,
          new LinkedMultiValueMap<>(),
          1000,
          pageable
      );

      var pagination = result.getResult().getPagination();

      Assertions.assertThat(pagination.getPage()).isEqualTo(7);
      Assertions.assertThat(pagination.getSize()).isEqualTo(20);
      Assertions.assertThat(pagination.getTotalElements()).isEqualTo(156);
      Assertions.assertThat(pagination.getTotalPages()).isEqualTo(8);
      Assertions.assertThat(pagination.isHasNext()).isFalse();
      Assertions.assertThat(pagination.isHasPrevious()).isTrue();
      Assertions.assertThat(pagination.isFirst()).isFalse();
      Assertions.assertThat(pagination.isLast()).isTrue();

      // Verify content size matches partial last page
      Assertions.assertThat(result.getResult().getContent()).hasSize(16);
    }

    @Test
    public void singlePageHasCorrectMetadata() {
      // All results fit on one page
      FacetSolrResponse solrResponse = createSolrResponse(15, 0, 15);
      Pageable pageable = PageRequest.of(0, 20);

      var result = FacetResponseDTO.from(
          solrResponse,
          new LinkedMultiValueMap<>(),
          1000,
          pageable
      );

      var pagination = result.getResult().getPagination();

      Assertions.assertThat(pagination.getPage()).isEqualTo(0);
      Assertions.assertThat(pagination.getTotalPages()).isEqualTo(1);
      Assertions.assertThat(pagination.isHasNext()).isFalse();
      Assertions.assertThat(pagination.isHasPrevious()).isFalse();
      Assertions.assertThat(pagination.isFirst()).isTrue();
      Assertions.assertThat(pagination.isLast()).isTrue();
    }

    @Test
    public void emptyResultsHaveCorrectMetadata() {
      // No results found
      FacetSolrResponse solrResponse = createSolrResponse(0, 0, 0);
      Pageable pageable = PageRequest.of(0, 20);

      var result = FacetResponseDTO.from(
          solrResponse,
          new LinkedMultiValueMap<>(),
          1000,
          pageable
      );

      var pagination = result.getResult().getPagination();

      Assertions.assertThat(pagination.getTotalElements()).isEqualTo(0);
      Assertions.assertThat(pagination.getTotalPages()).isEqualTo(0);
      Assertions.assertThat(result.getResult().getContent()).isEmpty();
    }
  }

  @Nested
  class EdgeCases {

    @Test
    public void handlesExactlyFullPages() {
      // Exactly 100 results with 20 per page = 5 pages exactly
      FacetSolrResponse solrResponse = createSolrResponse(20, 0, 100);
      Pageable pageable = PageRequest.of(0, 20);

      var result = FacetResponseDTO.from(
          solrResponse,
          new LinkedMultiValueMap<>(),
          500,
          pageable
      );

      Assertions.assertThat(result.getResult().getPagination().getTotalPages())
          .isEqualTo(5);
    }

    @Test
    public void handlesLargePageSize() {
      // Page size larger than total results
      FacetSolrResponse solrResponse = createSolrResponse(10, 0, 10);
      Pageable pageable = PageRequest.of(0, 100);

      var result = FacetResponseDTO.from(
          solrResponse,
          new LinkedMultiValueMap<>(),
          1000,
          pageable
      );

      Assertions.assertThat(result.getResult().getPagination().getTotalPages())
          .isEqualTo(1);
    }

    @Test
    public void handlesSmallPageSize() {
      // Very small page size
      FacetSolrResponse solrResponse = createSolrResponse(1, 0, 100);
      Pageable pageable = PageRequest.of(0, 1);

      var result = FacetResponseDTO.from(
          solrResponse,
          new LinkedMultiValueMap<>(),
          1000,
          pageable
      );

      Assertions.assertThat(result.getResult().getPagination().getTotalPages())
          .isEqualTo(100);
    }

    @Test
    public void emptySelectedFacetsDoesNotCauseIssues() {
      FacetSolrResponse solrResponse = createSolrResponse(10, 0, 50);
      Pageable pageable = PageRequest.of(0, 20);

      var result = FacetResponseDTO.from(
          solrResponse,
          new LinkedMultiValueMap<>(),  // Empty facets
          1000,
          pageable
      );

      Assertions.assertThat(result.getSelectedFacets())
          .isNotNull()
          .isEmpty();
    }
  }

  @Nested
  class RealWorldScenarios {

    @Test
    public void typicalFacetedSearchScenario() {
      // User searches with filters: type=Brief, coverage=Wien
      // Results: 45 matches, viewing page 1 (second page)
      MultiValueMap<String, String> selectedFacets = new LinkedMultiValueMap<>();
      selectedFacets.add("type", "Brief");
      selectedFacets.add("coverage", "Wien");

      FacetSolrResponse solrResponse = createSolrResponse(20, 20, 45);
      Pageable pageable = PageRequest.of(1, 20);

      var result = FacetResponseDTO.from(
          solrResponse,
          selectedFacets,
          1000,  // 1000 total documents in project
          pageable
      );

      // Verify filtered vs unfiltered counts
      Assertions.assertThat(result.getResult().getPagination().getTotalElements())
          .isEqualTo(45);  // 45 match filters
      Assertions.assertThat(result.getTotalUnfilteredCount())
          .isEqualTo(1000);  // 1000 total in project

      // Verify pagination
      Assertions.assertThat(result.getResult().getPagination().getPage()).isEqualTo(1);
      Assertions.assertThat(result.getResult().getPagination().getTotalPages()).isEqualTo(3);

      // Verify facets
      Assertions.assertThat(result.getSelectedFacets())
          .containsKeys("type", "coverage");
    }
  }

  // Helper method to create SolrFacetedResponse for testing
  private FacetSolrResponse createSolrResponse(int numDocuments, int start, long totalFound) {
    List<SolrDocument> documents = IntStream.range(0, numDocuments)
        .mapToObj(i -> new SolrDocument())
        .collect(Collectors.toList());

    return FacetSolrResponse.builder()
        .documents(documents)
        .start(start)
        .numFound(totalFound)
        .facets(new HashMap<>())
        .build();
  }
}