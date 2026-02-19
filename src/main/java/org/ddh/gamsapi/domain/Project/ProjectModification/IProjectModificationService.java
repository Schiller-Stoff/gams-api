package org.ddh.gamsapi.domain.Project.ProjectModification;

public interface IProjectModificationService {

  /**
   * Finds the latest modification date of a project (based on own modified date AND referenced digital object AND referenced datastream modified dates).
   * @param projectAbbr The project abbreviation.
   * @return The latest modification date.
   */
  ProjectModification findLatestModificationDate(String projectAbbr);



}
