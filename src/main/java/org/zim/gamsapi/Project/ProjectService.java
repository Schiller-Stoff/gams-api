package org.zim.gamsapi.Project;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.zim.gamsapi.Project.exceptions.ProjectNotFoundException;
import org.zim.gamsapi.Project.interfaces.IProjectRepository;
import org.zim.gamsapi.Project.interfaces.IProjectService;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectService implements IProjectService {

  private final IProjectRepository projectRepository;

  @Override
  public Project saveProject(Project project) {
    return projectRepository.save(project);
  }

  @Override
  public Project findProject(String projectAbbr) {
    return projectRepository.findById(projectAbbr).orElseThrow(() -> {
      String msg = String.format("Failed to find project %s", projectAbbr);
      log.error(msg);
      return new ProjectNotFoundException(msg);
    });
  }
}
