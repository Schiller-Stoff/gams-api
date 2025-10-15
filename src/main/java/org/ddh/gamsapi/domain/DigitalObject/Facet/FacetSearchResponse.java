package org.ddh.gamsapi.domain.DigitalObject.Facet;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.Builder;
import lombok.Data;
import org.ddh.gamsapi.domain.DigitalObject.utils.dto.DigitalObjectSearchResultDTO;
import org.ddh.gamsapi.infrastructure.System.dto.PagedResponse;

import java.util.List;
import java.util.Map;

/**
 * Complete faceted search response
 */
@Data
@Builder
@JacksonXmlRootElement(localName = "facets")
public class FacetSearchResponse {

  @JsonProperty("facetResult")
  @JacksonXmlElementWrapper(localName = "facetResult")
  private PagedResponse<DigitalObjectSearchResultDTO> results;
  private Map<String, List<FacetValue>> availableFacets;
  private Map<String, List<String>> selectedFacets;
  private long filteredCount;
  private long totalUnfilteredCount;
  private FacetSearchMetrics metrics;
}
