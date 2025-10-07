package org.zim.gamsapi.Project.interfaces;

import org.zim.gamsapi.Project.Project;

import java.util.List;

public interface IProjectService {

  /**
   * Checks if a project with the given abbreviation exists AND
   * if the given digital object ID is contained in the projectAbbr.
   *
   * @param projectAbbr The abbreviation of the project to check.
   * @return true if matches, else false.
   */
  boolean exists(String projectAbbr);

  /**
   * Verifies that the project abbreviation matches the digital object ID.
   * @param projectAbbr The abbreviation of the project to check.
   * @param digitalObjectId The ID of the digital object to check against the project abbreviation.
   */
  void verifyProjectAbbrMatchesObjectId(String projectAbbr, String digitalObjectId);

  Project save(Project project);

  void deleteProject(Project project);

  Project findProject(String projectAbbr);

  List<Project> findAll();

  Project findByAbbr(String projectAbbr);

  /**
   * Allows to update a project. Usually used in conjunction with PATCH requests.
   * @param project New project information.
   * @return updated project
   */
  Project updateProject(Project project);

}
