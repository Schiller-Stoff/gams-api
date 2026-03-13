package org.ddh.gamsapi.domain.Project;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ddh.gamsapi.domain.Datastream.utils.interfaces.IDatastreamRepository;
import org.ddh.gamsapi.domain.DigitalObject.utils.interfaces.IDigitalObjectRepository;
import org.ddh.gamsapi.domain.Project.dto.ProjectDetailsDTO;
import org.ddh.gamsapi.domain.Project.dto.ProjectStatisticsDTO;
import org.ddh.gamsapi.domain.Project.exceptions.ProjectNotEmptyException;
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
  private final IDigitalObjectRepository digitalObjectRepository;
  private final IDatastreamRepository datastreamRepository;

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

    // Pre-check: fail fast with a domain-specific message before hitting the DB constraint
    if (digitalObjectRepository.existsByProject_ProjectAbbr(foundProject.getProjectAbbr())) {
      throw new ProjectNotEmptyException(
          "Cannot delete project '" + foundProject.getProjectAbbr()
              + "' because it still contains digital objects. Delete all objects first."
      );
    }

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
    foundProject.setTitle(project.getTitle());
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

  @Override
  @Transactional(readOnly = true)
  public ProjectDetailsDTO findProjectDetails(String projectAbbr) {

    Project project = projectRepository.findById(projectAbbr).orElseThrow(
        () -> new ProjectNotFoundException(
            "Cannot find project with abbreviation: " + projectAbbr
        )
    );

    long digitalObjectCount = digitalObjectRepository.countByProject_ProjectAbbr(projectAbbr);
    long datastreamCount = datastreamRepository.countByDigitalObject_Project_ProjectAbbr(projectAbbr);
    long totalStorageBytes = datastreamRepository.sumSizeByProjectAbbr(projectAbbr);

    ProjectStatisticsDTO statistics = ProjectStatisticsDTO.builder()
        .digitalObjectCount(digitalObjectCount)
        .datastreamCount(datastreamCount)
        .totalStorageBytes(totalStorageBytes)
        .build();

    return ProjectDetailsDTO.builder()
        .projectAbbr(project.getProjectAbbr())
        .description(project.getDescription())
        .created(project.getCreated())
        .modified(project.getModified())
        .createdBy(project.getCreatedBy())
        .modifiedBy(project.getModifiedBy())
        .statistics(statistics)
        .title(project.getTitle())
        .build();
  }

}
