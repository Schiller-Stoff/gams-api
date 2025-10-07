package org.zim.gamsapi.Project;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zim.gamsapi.Project.exceptions.ProjectAlreadyExistsException;
import org.zim.gamsapi.Project.exceptions.ProjectNotFoundException;
import org.zim.gamsapi.Project.exceptions.ProjectObjectMismatchException;
import org.zim.gamsapi.Project.interfaces.IProjectRepository;
import org.zim.gamsapi.Project.interfaces.IProjectService;
import org.zim.gamsapi.System.dto.PagedResponse;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectService implements IProjectService {

  private final IProjectRepository projectRepository;

  @Override
  @Transactional
  public Project save(Project project) {

    Optional<Project> projectOptional = projectRepository.findById(project.getProjectAbbr());
    if(projectOptional.isPresent()){
      String msg = String.format("Project %s does already exist. Aborting creation of project.", project.getProjectAbbr());
      log.error(msg);
      throw new ProjectAlreadyExistsException(msg);
    }

    Project savedProject = projectRepository.save(project);
    log.trace("Saved project {}", project);

    return savedProject;
  }


  @Override
  @Transactional
  public void deleteProject(Project project) {
    Project foundProject = projectRepository.findById(project.getProjectAbbr()).orElseThrow(() -> {
      String msg = String.format("Project %s not found. Cannot delete project", project.getProjectAbbr());
      log.error(msg);
      return new ProjectNotFoundException(msg);
    });

    log.trace("Found project {}", foundProject);
    projectRepository.delete(foundProject);
    log.info("Successfully deleted project {}", foundProject);
  }

  @Override
  public Project findProject(String projectAbbr) {
    return projectRepository.findById(projectAbbr).orElseThrow(() -> {
      String msg = String.format("Failed to find project %s", projectAbbr);
      log.error(msg);
      return new ProjectNotFoundException(msg);
    });
  }

  @Override
  public List<Project> findAll() {
    List<Project> projects = new ArrayList<>();
    projectRepository.findAll().forEach(projects::add);
    return projects;
  }

  @Override
  public Project findByAbbr(String projectAbbr) {
    return findProject(projectAbbr);
  }

  @Override
  @Transactional
  public Project updateProject(Project project) {
    Project foundProject =  projectRepository.findById(project.getProjectAbbr()).orElseThrow(() -> {
      String msg = String.format("Project %s not found. Cannot update project", project.getProjectAbbr());
      log.error(msg);
      return new ProjectNotFoundException(msg);
    });
    foundProject.setDescription(project.getDescription());
    Project savedProject = projectRepository.save(foundProject);
    log.trace("Successfully updated project {}", foundProject);
    return savedProject;
  }

  @Override
  public boolean exists(String projectAbbr) {
    return projectRepository.existsById(projectAbbr);
  }

  @Override
  public void verifyProjectAbbrMatchesObjectId(String projectAbbr, String digitalObjectId) {
    if(!digitalObjectId.startsWith(projectAbbr)){
      String msg = String.format("Project abbreviation %s does not match the digital object ID %s", projectAbbr, digitalObjectId);
      log.error(msg);
      throw new ProjectObjectMismatchException(msg);
    }
  }

  @Override
  public PagedResponse<Project> findAllPaged(Pageable pageable) {
    return PagedResponse.from(
        projectRepository.findAll(pageable)
    );
  }
}
