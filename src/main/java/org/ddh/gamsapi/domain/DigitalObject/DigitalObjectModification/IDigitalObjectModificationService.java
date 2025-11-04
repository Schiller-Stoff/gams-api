package org.ddh.gamsapi.domain.DigitalObject.DigitalObjectModification;

public interface IDigitalObjectModificationService {

  /**
   * Finds the latest modification date of a digital object.
   * (based on own modified date AND datastream modified dates)
   * @param digitalObjectId The digital object id.
   * @return The latest modification date.
   */
  DigitalObjectModification findLatestModificationDate(String projectAbbr, String digitalObjectId);

  /**
   * Finds the last modified date of a digital object.
   * @param projectAbbr The project abbreviation.
   * @param digitalObjectId The digital object id.
   * @return The last modified date of the digital object.
   */
  DigitalObjectModification findLastModifiedDate(String projectAbbr, String digitalObjectId);

  /**
   * Calculates the latest modification date of a digital object by comparing the last modified dates of the digital object itself,
   * and it's datastreams.
   * @param digitalObjectId The project abbreviation.
   * @return The latest modification date.
   */
  DigitalObjectModification calculateLatestModificationDate(String digitalObjectId);

}
