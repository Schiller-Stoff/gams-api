package org.ddh.gamsapi.application.Integration.ApiSearch.Fulltext;


import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import org.ddh.gamsapi.application.Integration.ApiSearch.ApiSearch;
import org.ddh.gamsapi.application.Integration.ApiSearch.ApiSearchProperties;
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
 * DTO for fulltext digital object search results with pagination and selected filters.
 * Includes:
 * - Paginated search results
 * - Currently selected Dublin Core filters
 * - Total unfiltered document count for the specified projects
 */
@Builder
@Data
public class FulltextDigitalObjectResultDto {

  /**
   * Paginated search results with complete pagination metadata.
   * Includes: page number, size, total elements, total pages, hasNext, hasPrevious, isFirst, isLast
   */
  @JsonProperty("results")
  @Schema(description = "Paginated search results with complete metadata")
  private PagedResponse<ApiSearch> results;

  /**
   * Currently selected dc filters.
   * Key: facet field name
   * Value: List of selected values for that field
   */
  @JsonProperty("selectedFilter")
  @Schema(description = "Currently selected dc filters")
  private Map<String, List<String>> selectedFilter;

  /**
   * Total number of documents across all specified projects (baseline, no filters applied).
   * Example: 1000 total documents in the project(s)
   * This allows UI to show "Showing 5 of 1000 results"
   */
  @JsonProperty("totalUnfilteredCount")
  @Schema(description = "Total documents in projects before any filters", example = "1000")
  private long totalUnfilteredCount;

  public static FulltextDigitalObjectResultDto from(
      FulltextSolrResponse fulltextSolrResponse,
      MultiValueMap<String, String> selectedFilter,
      int totalUnfilteredCount,
      Pageable pageable
  ) {

    // Step 1: Map Solr documents to domain objects (and add highlighting data)
    List<ApiSearch> searchResults = fulltextSolrResponse.getDocuments().stream()
        .map( solrDocument -> {
          var baseSearch = ApiSearch.from(solrDocument);
          // additionally apply highlighting if available
          String docId = (String) baseSearch.getProperty(ApiSearchProperties.OBJECT_ID.name);
          var highlightInfo = fulltextSolrResponse.getHighlighting().get(docId);
          // Add highlighting info if present
          if(!highlightInfo.isEmpty()){
            baseSearch.addProperty(FulltextRequestProperties.HIGHLIGHTING.name, highlightInfo);
          }
          return baseSearch;
        })
        .collect(Collectors.toList());

    // Step 2: Create Spring Page with complete pagination metadata
    // This automatically calculates: totalPages, hasNext, hasPrevious, isFirst, isLast
    Page<ApiSearch> page = new PageImpl<>(
        searchResults,                      // Content for this page
        pageable,                           // Pageable (contains page number, size, sort)
        fulltextSolrResponse.getNumFound()   // Total elements (for totalPages calculation)
    );

    // Step 3: Convert to standard PagedResponse wrapper
    PagedResponse<ApiSearch> pagedResults = PagedResponse.from(page);

    // Step 4: Convert selected facets from MultiValueMap to standard Map
    Map<String, List<String>> selectedFilterMap = new HashMap<>();
    selectedFilter.forEach((key, values) ->
        selectedFilterMap.put(key, new ArrayList<>(values))
    );

    // Step 5: Build complete response
    return FulltextDigitalObjectResultDto.builder()
        .results(pagedResults)
        .selectedFilter(selectedFilterMap)
        .totalUnfilteredCount(totalUnfilteredCount)
        .build();



  }

}
