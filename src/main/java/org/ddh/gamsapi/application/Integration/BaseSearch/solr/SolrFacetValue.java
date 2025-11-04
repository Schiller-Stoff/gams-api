package org.ddh.gamsapi.application.Integration.BaseSearch.solr;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a single facet value with its count.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SolrFacetValue {
  private String value;
  private long count;
  private boolean selected;
}
