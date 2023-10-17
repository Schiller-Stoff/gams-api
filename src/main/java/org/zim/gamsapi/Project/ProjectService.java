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
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectService implements IProjectService {

  private final IProjectRepository projectRepository;
  private final IUserRepository userRepository;

  @Override
  @Transactional
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
    project.setUsers(new HashSet<>(Set.of(user)));
    projectRepository.save(project);
    log.debug("Create project - updated user associations {}", project);

    // add project to user and save user
    userToSave.getProjects().add(project);
    userToSave.setProjects(new HashSet<>(userToSave.getProjects()));
    userToSave = userRepository.save(userToSave);
    log.debug("Create project - updated project associations {}", userToSave);
    return userToSave;
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

    // update users referenced in project AND projects referenced in user
    userRepository.findAll().forEach(user -> {
      // remove project association from user
      Set<Project> newProjects = user.getProjects().stream()
              .filter(assignedProject -> !assignedProject.getProjectAbbr().equals(project.getProjectAbbr()))
              .collect(Collectors.toSet());
      user.setProjects(new HashSet<>(newProjects));
      userRepository.save(user);

      // remove user association from project
      Set<User> newAssignedUsers =  foundProject.getUsers().stream()
              .filter(assignedUser -> !assignedUser.getUsername().equals(user.getUsername()))
              .collect(Collectors.toSet());
      foundProject.setUsers(new HashSet<>(newAssignedUsers));
      projectRepository.save(foundProject);
    });

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
}
