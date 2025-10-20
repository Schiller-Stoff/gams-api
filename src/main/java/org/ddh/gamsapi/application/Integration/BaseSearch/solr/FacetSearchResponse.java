package org.ddh.gamsapi.application.Integration.BaseSearch.solr;

import lombok.Builder;
import lombok.Data;
import org.ddh.gamsapi.application.Integration.BaseSearch.BaseSearch;
import org.ddh.gamsapi.domain.DigitalObject.Facet.FacetSearchMetrics;

import java.util.List;
import java.util.Map;

/**
 * Complete faceted search response wrapper.
 */
@Data
@Builder
public class FacetSearchResponse {
  private List<BaseSearch> results;
  private Map<String, List<FacetValue>> availableFacets;
  private Map<String, List<String>> selectedFacets;
  private long filteredCount;
  private long totalUnfilteredCount;
  private FacetSearchMetrics metrics;
}
