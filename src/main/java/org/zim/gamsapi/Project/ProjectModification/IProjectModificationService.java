package org.zim.gamsapi.Project.ProjectModification;

import java.time.LocalDateTime;
import java.util.Date;

public interface IProjectModificationService {

  ProjectModification findLatestModificationDate(String projectAbbr);

  /**
   * Calculates the latest modification date of a project by comparing the last modified dates of the project itself,
   * it's digital objects and it's datastreams.
   * @param projectAbbr The project abbreviation.
   * @return The latest modification date.
   */
  ProjectModification calculateLatestModificationDate(String projectAbbr);

}
