package org.zim.gamsapi.DigitalObject.Facet;

import lombok.Builder;
import lombok.Data;

/**
 * Simple facet value with count
 */
@Data
@Builder
public class FacetValue {
  private String value;
  private String label;
  private long count;
  private boolean selected;

  public boolean isSelectable() {
    return count > 0;
  }
}