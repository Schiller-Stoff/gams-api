package org.zim.gamsapi.Project;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.*;
import org.zim.gamsapi.Project.interfaces.IProjectService;
import org.zim.gamsapi.User.User;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Controller
@RequestMapping({"/api/v1/projects", "/api/v1/projects/"})
public class ProjectController {

  private final IProjectService projectService;

  @PutMapping(path = "/{projectAbbr}")
  public String createProject(Project project, User user, Model model){
    User updatedUser = projectService.createNewProject(project, user);
    model.addAttribute(updatedUser);
    return "User/userprojects";
  }

  @DeleteMapping(path = "/{projectAbbr}")
  @ResponseBody
  public void deleteProject(Project project){
    projectService.deleteProject(project);
  }

  @GetMapping
  @ResponseBody
  public List<Project> showProjects(){
    return projectService.findAll();
  }

  @GetMapping(produces = MimeTypeUtils.TEXT_HTML_VALUE)
  public String showProjectsViaWebClient(Model model){
    List<Project> projects = projectService.findAll();
    model.addAttribute("projects", projects);
    return "Project/show_all";
  }

}
