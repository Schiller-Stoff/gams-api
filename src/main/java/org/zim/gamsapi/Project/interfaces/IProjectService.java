package org.zim.gamsapi.Project.interfaces;

import org.zim.gamsapi.Project.Project;

public interface IProjectService {


  Project saveProject(Project project);

  Project findProject(String projectAbbr);

}
