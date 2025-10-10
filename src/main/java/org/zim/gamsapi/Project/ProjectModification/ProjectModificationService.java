package org.zim.gamsapi.Project.ProjectModification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zim.gamsapi.Datastream.utils.interfaces.IDatastreamRepository;
import org.zim.gamsapi.DigitalObject.utils.interfaces.IDigitalObjectRepository;
import org.zim.gamsapi.Project.exceptions.ProjectNotFoundException;
import org.zim.gamsapi.Project.interfaces.IProjectRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectModificationService implements IProjectModificationService {
  private final IProjectRepository projectRepository;
  private final IDigitalObjectRepository digitalObjectRepository;
  private final IDatastreamRepository datastreamRepository;

  @Transactional
  @Override
  public ProjectModification findContentLatestModificationDate(String projectAbbr) {
    var foundProject = projectRepository.findById(projectAbbr).orElseThrow(() -> {
      String msg = String.format("Project with project-abbreviation %s does not exist", projectAbbr);
      log.warn(msg);
      return new ProjectNotFoundException(msg);
    });

    ProjectModification projectModification = new ProjectModification();
    projectModification.setProjectAbbr(projectAbbr);
    projectModification.setLatestModificationDate(foundProject.getContentLastModified());
    return projectModification;
  }

  @Transactional
  public ProjectModification findLatestModificationDate(String projectAbbr) {

    var foundProject = projectRepository.findById(projectAbbr).orElseThrow(() -> {
      String msg = String.format("Project with project-abbreviation %s does not exist", projectAbbr);
      log.warn(msg);
      return new ProjectNotFoundException(msg);
    });

    ProjectModification projectModification = new ProjectModification();
    projectModification.setProjectAbbr(projectAbbr);
    projectModification.setLatestModificationDate(foundProject.getModified());
    return projectModification;
  }


}
