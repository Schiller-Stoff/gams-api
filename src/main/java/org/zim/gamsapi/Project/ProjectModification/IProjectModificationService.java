package org.zim.gamsapi.Project.ProjectModification;

public interface IProjectModificationService {

  /**
   * Finds the latest modification date of a project (based on own modified date AND referenced digital object AND referenced datastream modified dates).
   * @param projectAbbr The project abbreviation.
   * @return The latest modification date.
   */
  ProjectModification findLatestModificationDate(String projectAbbr);

  /**
   * Calculates the latest modification date of a project by comparing the last modified dates of the project itself,
   * it's digital objects and it's datastreams.
   * @param projectAbbr The project abbreviation.
   * @return The latest modification date.
   */
  ProjectModification calculateLatestModificationDate(String projectAbbr);

}
