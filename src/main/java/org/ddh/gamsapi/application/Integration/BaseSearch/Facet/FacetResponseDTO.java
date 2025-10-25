package org.ddh.gamsapi.application.Integration.BaseSearch.Facet;

import lombok.Builder;
import lombok.Data;
import org.ddh.gamsapi.application.Integration.BaseSearch.BaseSearch;
import org.ddh.gamsapi.application.Integration.BaseSearch.solr.SolrFacetValue;
import org.ddh.gamsapi.application.Integration.BaseSearch.solr.SolrFacetedResponse;
import org.ddh.gamsapi.domain.DigitalObject.Facet.FacetSearchMetrics;
import org.springframework.util.MultiValueMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Complete faceted search response wrapper.
 */
@Data
@Builder
public class FacetResponseDTO {
  private List<BaseSearch> results;
  private Map<String, List<SolrFacetValue>> availableFacets;
  private Map<String, List<String>> selectedFacets;

  /**
   * Number of results matching the current query + filters.
   * Example: 5 results when filtering by type=Brief
   */
  private long filteredCount;

  /**
   * Total number of documents across all specified projects (baseline, no filters applied).
   * Example: 1000 total documents in the project(s)
   * This allows UI to show "Showing 5 of 1000 results"
   */
  private long totalUnfilteredCount;

  /**
   * Starting offset for pagination (0-based).
   */
  private long start;


  /**
   * Builds a FacetSearchResponse instance from given parsed solr response
   * @param solrFacetedResponse response from solr
   * @param selectedFacets selected facets during request building to solr
   * @return response for faceted search
   */
  public static FacetResponseDTO from(
      SolrFacetedResponse solrFacetedResponse,
      MultiValueMap<String, String> selectedFacets,
      int totalUnfilteredCount
  ){

    var selectedFacetsAsNormalMap = new HashMap<String, List<String>>();
    selectedFacets.forEach((s, strings) -> {
      selectedFacetsAsNormalMap.put(s, new ArrayList<>(strings));
    });

    List<BaseSearch> mapped = new ArrayList<>();
    solrFacetedResponse.getDocuments().forEach((solrDocument) -> {
      mapped.add(BaseSearch.from(solrDocument));
    });

    return FacetResponseDTO.builder()
        .results(mapped)
        .availableFacets(solrFacetedResponse.getFacets())
        .selectedFacets(selectedFacetsAsNormalMap)
        .filteredCount(solrFacetedResponse.getNumFound())
        .totalUnfilteredCount(totalUnfilteredCount)
        .start(solrFacetedResponse.getStart())
        .build();

  }
}
