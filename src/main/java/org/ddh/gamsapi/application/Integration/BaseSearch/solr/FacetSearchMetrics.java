package org.ddh.gamsapi.application.Integration.BaseSearch.solr;

import lombok.Builder;
import lombok.Data;

/**
 * Performance metrics for faceted search.
 */
@Data
@Builder
public class FacetSearchMetrics {
  private long searchTimeMs;
  private long facetCountTimeMs;
  private long totalTimeMs;
  private int numberOfFacetFields;
  private String performanceNote;
}
