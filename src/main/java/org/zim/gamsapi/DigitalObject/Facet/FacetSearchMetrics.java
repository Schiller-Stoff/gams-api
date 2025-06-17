package org.zim.gamsapi.DigitalObject.Facet;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FacetSearchMetrics {
  private long searchTimeMs;
  private long facetCountTimeMs;
  private long totalTimeMs;
  private int numberOfFacetFields;
  private String performanceNote;
}
