package org.zim.gamsapi.Project.interfaces;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.zim.gamsapi.Project.Project;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.Optional;

public interface IProjectRepository extends CrudRepository<Project, String> {

  /**
   * Finds the last modified date (!CAREFUL: Only considers direct changes to the project repository = Project Table)
   * of a project by its abbreviation. Changes done to referenced entities are not considered.
   * @param projectAbbr The abbreviation of the project.
   * @return The last modified date of the project.
   */
  @Query("SELECT p.modified FROM Project p WHERE p.projectAbbr = :projectAbbr")
  Optional<Date> findLastModifiedDateByProjectAbbr(@Param("projectAbbr") String projectAbbr);

}


