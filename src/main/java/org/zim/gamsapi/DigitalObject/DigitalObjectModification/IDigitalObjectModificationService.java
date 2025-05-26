package org.zim.gamsapi.DigitalObject.DigitalObjectModification;

public interface IDigitalObjectModificationService {

  /**
   * Finds the latest modification date of a digital object.
   * (based on own modified date AND datastream modified dates)
   * @param digitalObjectId The digital object id.
   * @return The latest modification date.
   */
  DigitalObjectModification findLatestModificationDate(String digitalObjectId);

  /**
   * Calculates the latest modification date of a digital object by comparing the last modified dates of the digital object itself,
   * and it's datastreams.
   * @param digitalObjectId The project abbreviation.
   * @return The latest modification date.
   */
  DigitalObjectModification calculateLatestModificationDate(String digitalObjectId);

}
