package org.zim.gamsapi.domain.DigitalObject.Facet;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.Builder;
import lombok.Data;

/**
 * Simple facet value with count
 */
@Data
@Builder
@JacksonXmlRootElement(localName = "facet")
public class FacetValue {
  private String value;
  private String label;
  private long count;
  private boolean selected;

  public boolean isSelectable() {
    return count > 0;
  }
}