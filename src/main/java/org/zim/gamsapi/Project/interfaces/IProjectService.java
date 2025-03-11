package org.zim.gamsapi.Project.interfaces;

import org.zim.gamsapi.Project.Project;

import java.util.List;

public interface IProjectService {

  Project save(Project project);

  void deleteProject(Project project);

  Project findProject(String projectAbbr);

  List<Project> findAll();

Project findByAbbr(String projectAbbr);

}
