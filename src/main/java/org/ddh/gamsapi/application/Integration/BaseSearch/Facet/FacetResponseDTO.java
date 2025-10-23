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
  private long filteredCount;
  private long totalUnfilteredCount;
  private FacetSearchMetrics metrics;
  private long start;
  private long totalCount;


  /**
   * Builds a FacetSearchResponse instance from given parsed solr response
   * @param solrFacetedResponse response from solr
   * @param selectedFacets selected facets during request building to solr
   * @return response for faceted search
   */
  public static FacetResponseDTO from(
      SolrFacetedResponse solrFacetedResponse,
      MultiValueMap<String, String> selectedFacets
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
        .totalUnfilteredCount(solrFacetedResponse.getTotalCount())
        .start(solrFacetedResponse.getStart())
        .totalCount(solrFacetedResponse.getTotalCount())
        .build();

  }
}
