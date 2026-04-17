package org.ddh.gamsapi.domain.Project.ProjectModification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ddh.gamsapi.domain.Project.exceptions.ProjectNotFoundException;
import org.ddh.gamsapi.domain.Project.interfaces.IProjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectModificationService implements IProjectModificationService {
  private final IProjectRepository projectRepository;

  @Transactional
  public ProjectModification findLatestModificationDate(String projectAbbr) {

    var foundProject = projectRepository.findById(projectAbbr).orElseThrow(() -> new ProjectNotFoundException(
        "Project with project-abbreviation" + projectAbbr + "does not exist"
    ));

    ProjectModification projectModification = new ProjectModification();
    projectModification.setProjectAbbr(projectAbbr);
    projectModification.setLatestModificationDate(foundProject.getModified());
    return projectModification;
  }


}
