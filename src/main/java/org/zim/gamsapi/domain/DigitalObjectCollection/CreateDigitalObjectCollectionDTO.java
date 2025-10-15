package org.zim.gamsapi.domain.DigitalObjectCollection;

import lombok.Getter;
import lombok.Setter;

/**
 * DTO for creating a GAMS collection.
 */
@Getter
@Setter
public class CreateDigitalObjectCollectionDTO {
  private String projectAbbr;
  private String title;
  private String description;
}
