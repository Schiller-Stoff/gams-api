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
   * Find the last modified date of a project by its abbreviation.
   * @param projectAbbr The abbreviation of the project.
   * @return The last modified date of the project.
   */
  @Query("SELECT p.modified FROM Project p WHERE p.projectAbbr = :projectAbbr")
  Optional<Date> findLastModifiedDateByProjectAbbr(@Param("projectAbbr") String projectAbbr);

}


