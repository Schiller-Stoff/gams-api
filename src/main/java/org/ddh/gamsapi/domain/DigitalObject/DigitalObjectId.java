package org.ddh.gamsapi.domain.DigitalObject;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Container for a digital object id.
 * Allows to extract the related projectAbbr.
 */
@Getter
@AllArgsConstructor
public class DigitalObjectId {

  private String id;

  /**
   * Allows to return the project abbreviation of the encapsulated object id
   * @return projectAbbr of the id TODO replace somewhere else + test!
   */
  public String deriveProjectAbbr(){
    // return everything before first "."
    return id.substring(0, id.indexOf("."));
  }

}
