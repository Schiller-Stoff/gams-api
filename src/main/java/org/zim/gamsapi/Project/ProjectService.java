package org.zim.gamsapi.Project;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.zim.gamsapi.Project.interfaces.IProjectService;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectService implements IProjectService {
  @Override
  public Project findPlain(Project project) {
    return project;
  }

  @Override
  public Project getUserProjectByEntity(Project project) {
    return project;
  }
}
