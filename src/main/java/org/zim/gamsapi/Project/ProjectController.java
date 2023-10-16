package org.zim.gamsapi.Project;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.zim.gamsapi.Project.interfaces.IProjectService;
import org.zim.gamsapi.User.User;
import org.zim.gamsapi.User.interfaces.IUserService;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Controller
@RequestMapping({"/api/v1/projects/{projectAbbr}", "/api/v1/projects/{projectAbbr}/"})
public class ProjectController {

  private final IProjectService projectService;
  private final IUserService userService;

  @PutMapping
  public String createProject(Project project, User user, Model model){
    User updatedUser = projectService.createNewProject(project, user);
    model.addAttribute(updatedUser);
    return "User/userprojects";
  }

  @DeleteMapping
  public String deleteProject(Project project, User user, Model model){
    user = userService.findByUsername(user.getUsername());
    projectService.deleteProject(project);
    Set<Project> filteredProjects = user.getProjects().stream()
            .filter(projectToFilter -> !projectToFilter.getProjectAbbr().equals(project.getProjectAbbr()))
            .collect(Collectors.toSet());
    user.setProjects(filteredProjects);
    userService.saveUser(user);
    model.addAttribute(user);
    return "User/userprojects";
  }

}
