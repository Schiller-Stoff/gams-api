package org.zim.gamsapi.Project;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.zim.gamsapi.Project.interfaces.IProjectService;

@Slf4j
@RequiredArgsConstructor
@Controller
@RequestMapping({"/api/v1/projects/{projectAbbr}", "/api/v1/projects/{projectAbbr}/"})
public class ProjectController {

  private final IProjectService projectService;

  @GetMapping
  public String showProjectDigitalObjects(Model model, @ModelAttribute Project project) {
    Project curProject = projectService.findPlain(project);
    model.addAttribute(curProject);
    return "Project/show";
  }

}
