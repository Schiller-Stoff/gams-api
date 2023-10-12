package org.zim.gamsapi.Project;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.zim.gamsapi.Project.interfaces.IProjectService;

@Slf4j
@RequiredArgsConstructor
@Controller
@RequestMapping({"/api/v1/projects/{projectAbbr}", "/api/v1/projects/{projectAbbr}/"})
public class ProjectController {

  private final IProjectService projectService;

  @PutMapping
  public String createProject(Project project, Model model){
    Project savedProject = projectService.saveProject(project);
    model.addAttribute(savedProject);
    return "User/userprojects";
  }


}
