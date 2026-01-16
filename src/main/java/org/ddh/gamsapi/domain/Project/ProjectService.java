package org.ddh.gamsapi.domain.Project;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ddh.gamsapi.domain.Project.interfaces.ProjectIdView;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.ddh.gamsapi.domain.Project.exceptions.ProjectAlreadyExistsException;
import org.ddh.gamsapi.domain.Project.exceptions.ProjectNotFoundException;
import org.ddh.gamsapi.domain.Project.exceptions.ProjectObjectMismatchException;
import org.ddh.gamsapi.domain.Project.interfaces.IProjectRepository;
import org.ddh.gamsapi.domain.Project.interfaces.IProjectService;
import org.ddh.gamsapi.infrastructure.System.dto.PagedResponse;

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
      throw new ProjectAlreadyExistsException(
          "Project " + project.getProjectAbbr() + " already exists"
      );
    }

    Project savedProject = projectRepository.save(project);
    log.info("Saved project {}", project);
    return savedProject;
  }


  @Override
  @Transactional
  public void deleteProject(Project project) {
    Project foundProject = projectRepository.findById(project.getProjectAbbr()).orElseThrow(() -> new ProjectNotFoundException(
        "Project " + project.getProjectAbbr() + " not found. Cannot delete project"
    ));

    log.trace("Found project {}", foundProject);
    projectRepository.delete(foundProject);
    log.info("Successfully deleted project {}", foundProject);
  }

  @Override
  public Project findProject(String projectAbbr) {
    return projectRepository.findById(projectAbbr).orElseThrow(() -> new ProjectNotFoundException(
        "Failed to find project " + projectAbbr
    ));
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
    Project foundProject =  projectRepository.findById(project.getProjectAbbr()).orElseThrow(() -> new ProjectNotFoundException(
        "Project " + project.getProjectAbbr() + " not found. Cannot update project"
    ));
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
      throw new ProjectObjectMismatchException(
          "Project abbreviation does not match digital object ID: " + digitalObjectId + ". For project: " + projectAbbr
      );
    }
  }

  @Override
  public PagedResponse<Project> findAllPaged(Pageable pageable) {
    return PagedResponse.from(
        projectRepository.findAll(pageable)
    );
  }

  @Override
  @Transactional(readOnly = true)
  public List<String> findAllProjectAbbrs() {
    return projectRepository.findAllProjectedByOrderByProjectAbbrAsc()
        .stream()
        .map(ProjectIdView::getProjectAbbr)
        .toList();
  }

}
