package org.zim.gamsapi.Project;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zim.gamsapi.Project.exceptions.ProjectAlreadyExistsException;
import org.zim.gamsapi.Project.exceptions.ProjectNotFoundException;
import org.zim.gamsapi.Project.interfaces.IProjectRepository;
import org.zim.gamsapi.Project.interfaces.IProjectService;
import org.zim.gamsapi.User.User;
import org.zim.gamsapi.User.exceptions.UserNotFoundException;
import org.zim.gamsapi.User.interfaces.IUserRepository;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectService implements IProjectService {

  private final IProjectRepository projectRepository;
  private final IUserRepository userRepository;

  @Override
  public User createNewProject(Project project, User user) {

    Optional<Project> projectOptional = projectRepository.findById(project.getProjectAbbr());
    if(projectOptional.isPresent()){
      String msg = String.format("Project %s does already exist. Aborting creation of project.", project.getProjectAbbr());
      log.error(msg);
      throw new ProjectAlreadyExistsException(msg);
    }

    Optional<User> userOptional = userRepository.findByUsername(user.getUsername());
    if(userOptional.isEmpty()){
      String msg = String.format("Cannot find user with name %s. Aborting creation of project.", user.getUsername());
      log.error(msg);
      throw new UserNotFoundException(msg);
    }

    User userToSave = userOptional.get();
    // add user to project and save project
    project.setUsers(List.of(user));
    projectRepository.save(project);
    // add project to user and save user
    userToSave.getProjects().add(project);
    userToSave = userRepository.save(userToSave);
    return userToSave;
  }

  @Override
  @Transactional
  public Project saveProject(Project project) {
    return projectRepository.save(project);
  }

  @Override
  @Transactional
  public void deleteProject(Project project) {
    projectRepository.findById(project.getProjectAbbr()).orElseThrow(() -> {
      String msg = String.format("Project %s not found. Cannot delete project", project.getProjectAbbr());
      log.error(msg);
      return new ProjectNotFoundException(msg);
    });
    projectRepository.delete(project);
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
