package org.zim.gamsapi.Project.interfaces;

import org.zim.gamsapi.Project.Project;

public interface IProjectService {

  /**
   * Returns given Project as ProjectViewModel. Just contains
   * information about project name (contained digital object whatsoever is omitted)
   * @param project Project domain object
   * @return Project view model
   */
  Project findPlain(Project project);

  /**
   * Returns a GAMS project.
   * @param project project to be returned
   * @return current project
   */
  public Project getUserProjectByEntity(Project project);

  Project saveProject(Project project);

}
