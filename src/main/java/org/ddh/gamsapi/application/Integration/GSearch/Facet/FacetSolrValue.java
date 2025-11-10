package org.ddh.gamsapi.application.Integration.GSearch.Facet;

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
public class FacetSolrValue {
  private String value;
  private long count;
  private boolean selected;
}
