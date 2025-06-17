package org.zim.gamsapi.DigitalObject.Facet;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.domain.Page;
import org.zim.gamsapi.DigitalObject.dto.DigitalObjectSearchResultDTO;

import java.util.List;
import java.util.Map;

/**
 * Complete faceted search response
 */
@Data
@Builder
public class FacetSearchResponse {
  private Page<DigitalObjectSearchResultDTO> results;
  private Map<String, List<FacetValue>> availableFacets;
  private Map<String, List<String>> selectedFacets;
  private long filteredCount;
  private long totalUnfilteredCount;
  private FacetSearchMetrics metrics;
}
