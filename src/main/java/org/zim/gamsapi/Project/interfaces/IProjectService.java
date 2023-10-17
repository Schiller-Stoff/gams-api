package org.zim.gamsapi.Project.interfaces;

import org.zim.gamsapi.Project.Project;
import org.zim.gamsapi.User.User;

public interface IProjectService {

  User createNewProject(Project project, User user);

  void deleteProject(Project project);

  Project findProject(String projectAbbr);

}
