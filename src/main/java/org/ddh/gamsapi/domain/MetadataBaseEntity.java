package org.ddh.gamsapi.domain;

import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Handles shared metadata fields between DigitalObject and Datastream
 *
 * https://www.dublincore.org/
 */
@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MetadataBaseEntity {

  /**
   * Title of digital object / datastream
   */
  @NotEmpty
  private String title;

  /**
   * Rights statement of digital object / datastream
   */
  @NotEmpty
  private String rights;


  /**
   * Creator of digital object or datastream
   */
  @NotEmpty
  private String creator;

  /**
   * Description if the digital object or datastream
   */
  @Size(max = 2000)
  private String description;

}
