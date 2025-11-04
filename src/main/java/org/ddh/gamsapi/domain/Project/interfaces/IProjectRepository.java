package org.ddh.gamsapi.domain.Project.interfaces;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.ddh.gamsapi.domain.Project.Project;
import java.util.Date;
import java.util.List;
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

  Page<Project> findAll(Pageable pageable);

  /**
   * Returns all project abbreviations sorted alphabetically in ascending order.
   * This method uses a projection interface to efficiently fetch only the projectAbbr field.
   *
   * Performance consideration: For hundreds of projects, this is highly efficient.
   * The query will only SELECT the project_abbr column and sort at the database level.
   *
   * @return List of all project abbreviations sorted A-Z
   */
  List<ProjectIdView> findAllProjectedByOrderByProjectAbbrAsc();

}


