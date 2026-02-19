package org.ddh.gamsapi.domain.DigitalObject.DigitalObjectModification;

public interface IDigitalObjectModificationService {

  /**
   * Finds the last modified date of a digital object.
   * @param projectAbbr The project abbreviation.
   * @param digitalObjectId The digital object id.
   * @return The last modified date of the digital object.
   */
  DigitalObjectModification findLastModifiedDate(String projectAbbr, String digitalObjectId);


}
