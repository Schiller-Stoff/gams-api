package org.zim.gamsapi.domain.GAMSCollection;

import lombok.Getter;
import lombok.Setter;

/**
 * DTO for creating a GAMS collection.
 */
@Getter
@Setter
public class CreateGAMSCollectionDTO {
  private String projectAbbr;
  private String title;
  private String description;
}
