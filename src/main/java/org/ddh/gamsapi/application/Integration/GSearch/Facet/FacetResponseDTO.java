package org.ddh.gamsapi.application.Integration.GSearch.Facet;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import org.ddh.gamsapi.application.Integration.GSearch.GSearch;
import org.ddh.gamsapi.infrastructure.System.dto.PagedResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.util.MultiValueMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Complete faceted search response wrapper with proper Spring pagination support.
 *
 * This DTO provides:
 * - Standard Spring Page metadata (page number, total pages, hasNext, etc.)
 * - Facet-specific information (available facets, selected filters)
 * - Unfiltered document count for "X of Y" displays
 */
@Data
@Builder
@Schema(description = "Faceted search response with pagination and facet information")
public class FacetResponseDTO {

  /**
   * Paginated search results with complete pagination metadata.
   * Includes: page number, size, total elements, total pages, hasNext, hasPrevious, isFirst, isLast
   */
  @JsonProperty("result")
  @Schema(description = "Paginated search results with complete metadata")
  private PagedResponse<GSearch> result;

  /**
   * Available facet values with their counts for drill-down navigation.
   * Key: facet field name (e.g., "type", "coverage")
   * Value: List of possible values with their document counts
   */
  @JsonProperty("availableFacets")
  @Schema(description = "Available facet values with counts for each facet field")
  private Map<String, List<FacetSolrValue>> availableFacets;

  /**
   * Currently selected facet filters.
   * Key: facet field name
   * Value: List of selected values for that field
   */
  @JsonProperty("selectedFacets")
  @Schema(description = "Currently selected facet filters")
  private Map<String, List<String>> selectedFacets;

  /**
   * Total number of documents across all specified projects (baseline, no filters applied).
   * Example: 1000 total documents in the project(s)
   * This allows UI to show "Showing 5 of 1000 results"
   */
  @JsonProperty("totalUnfilteredCount")
  @Schema(description = "Total documents in projects before any filters", example = "1000")
  private long totalUnfilteredCount;

  /**
   * Builds a FacetResponseDTO from Solr response with proper Spring pagination.
   *
   * @param facetSolrResponse Parsed Solr response containing documents and facets
   * @param selectedFacets Selected facets used in the query
   * @param totalUnfilteredCount Total documents in projects (no filters)
   * @param pageable Pagination parameters used in the query
   * @return Complete faceted search response with pagination
   */
  public static FacetResponseDTO from(
      FacetSolrResponse facetSolrResponse,
      MultiValueMap<String, String> selectedFacets,
      int totalUnfilteredCount,
      Pageable pageable
  ) {

    // Step 1: Map Solr documents to domain objects
    List<GSearch> searchResults = facetSolrResponse.getDocuments().stream()
        .map(GSearch::from)
        .collect(Collectors.toList());

    // Step 2: Create Spring Page with complete pagination metadata
    // This automatically calculates: totalPages, hasNext, hasPrevious, isFirst, isLast
    Page<GSearch> page = new PageImpl<>(
        searchResults,                      // Content for this page
        pageable,                           // Pageable (contains page number, size, sort)
        facetSolrResponse.getNumFound()   // Total elements (for totalPages calculation)
    );

    // Step 3: Convert to standard PagedResponse wrapper
    PagedResponse<GSearch> pagedResults = PagedResponse.from(page);

    // Step 4: Convert selected facets from MultiValueMap to standard Map
    Map<String, List<String>> selectedFacetsMap = new HashMap<>();
    selectedFacets.forEach((key, values) ->
        selectedFacetsMap.put(key, new ArrayList<>(values))
    );

    // Step 5: Build complete response
    return FacetResponseDTO.builder()
        .result(pagedResults)
        .availableFacets(facetSolrResponse.getFacets())
        .selectedFacets(selectedFacetsMap)
        .totalUnfilteredCount(totalUnfilteredCount)
        .build();
  }
}