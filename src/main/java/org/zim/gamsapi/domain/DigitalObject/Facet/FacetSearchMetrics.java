package org.zim.gamsapi.domain.DigitalObject.Facet;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JacksonXmlRootElement(localName = "metrics")
public class FacetSearchMetrics {
  private long searchTimeMs;
  private long facetCountTimeMs;
  private long totalTimeMs;
  private int numberOfFacetFields;
  private String performanceNote;
}
