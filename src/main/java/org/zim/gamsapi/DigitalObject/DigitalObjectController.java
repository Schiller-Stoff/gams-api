package org.zim.gamsapi.DigitalObject;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.*;
import org.zim.gamsapi.Project.Project;
import org.zim.gamsapi.Project.interfaces.IProjectService;
import org.zim.gamsapi.System.utils.ControllerUtils;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping(value = {"/api/v1/projects/{projectAbbr}/objects", "/api/v1/projects/{projectAbbr}/objects/"})
@Slf4j
@RequiredArgsConstructor
public class DigitalObjectController {

  private final DigitalObjectService digitalObjectService;
  private final IProjectService projectService;

  @GetMapping(value = {"/{pid}", "/{pid}/"}, produces = MimeTypeUtils.APPLICATION_JSON_VALUE)
  @ResponseBody
  public DigitalObject getObjectJson(DigitalObject digitalObject, Project project, Model model) {
    DigitalObject foundObject = digitalObjectService.findByPid(digitalObject.getPid());
    model.addAttribute(foundObject);
    log.info("Found digital object {} for project {}", digitalObject, project);
    return foundObject;
  }

  @GetMapping(value = {"/{pid}", "/{pid}/"})
  public String getObject(DigitalObject digitalObject, Project project, Model model) {
    DigitalObject foundDigitalObject = digitalObjectService.findByPid(digitalObject.getPid());
    Project curProject = projectService.findPlain(project);
    model.addAttribute("do", foundDigitalObject);
    model.addAttribute(curProject);
    log.info("Found digital object {} for project {}", foundDigitalObject, project.getProjectAbbr());
    return "DigitalObject/show";
  }


  @GetMapping
  public String getProjectObjects(
          Model model,
          Project project,
          // for pagination
          @RequestParam(defaultValue = "0") int pageIndex,
          @RequestParam(defaultValue = "10") int pageSize,
          @RequestParam(defaultValue = "") String pid,
          @RequestParam(defaultValue = "pid") String sortBy

  ) {
    //Page<DigitalObject> digitalObjects = digitalObjectService.findAllByProjectAbbr(project.getProjectAbbr(), PageRequest.of(pageIndex, pageSize, Sort.by("pid")));
    Page<DigitalObject> digitalObjects = digitalObjectService.findAllByProjectAbbr(
            project.getProjectAbbr(),
            pid,
            PageRequest.of(pageIndex, pageSize, Sort.by(sortBy))
    );

    Project curProject = this.projectService.getUserProjectByEntity(project);
    model.addAttribute("digitalObjects", digitalObjects.toList());
    model.addAttribute(curProject);
    model.addAttribute("pageSize", pageSize);
    model.addAttribute("pageIndex", pageIndex);
    model.addAttribute("totalItems", digitalObjects.getTotalElements());
    model.addAttribute("totalPages", digitalObjects.getTotalPages());
    model.addAttribute("searchPid", pid);
    model.addAttribute("sortBy", sortBy);

    //log.info("Found objects {} for project {}", digitalObjects, project);
    return "DigitalObject/show_all";
  }

  @GetMapping(produces = MimeTypeUtils.APPLICATION_JSON_VALUE)
  @ResponseBody
  public List<DigitalObject> getProjectObjectsJson(
          Model model,
          Project project,
          // for pagenination
          @RequestParam(defaultValue = "0") int pageIndex,
          @RequestParam(defaultValue = "15") int pageSize
  ) {
    Project curProject = this.projectService.getUserProjectByEntity(project);
    model.addAttribute(curProject);
    log.info("Found objects for project {}", project.getProjectAbbr());
    return digitalObjectService.findAllByProjectAbbr(project.getProjectAbbr(), PageRequest.of(pageIndex, pageSize, Sort.by("pid"))).toList();
  }

  @PutMapping(value = {"/{pid}", "/{pid}/"})
  public String createObject(
          DigitalObject digitalObject,
          Project project,
          Model model,
          @RequestHeader Map<String, String> requestHeader
  ) {

    DigitalObject savedObject = digitalObjectService.save(digitalObject);
    model.addAttribute("do", savedObject);
    log.info("Created object {} for project {}", savedObject, project);

    // needed to consider proxy forwarding
    String origin = ControllerUtils.resolveProxiedOrigin(requestHeader);
    return "redirect:" + origin + "api/v1/projects/" + project.getProjectAbbr() + "/objects/" + savedObject.getPid();
  }

  @DeleteMapping(value = {"/{pid}", "/{pid}/"})
  public String deleteObject(
          DigitalObject digitalObject,
          Project project,
          @RequestHeader Map<String, String> requestHeader) {
    this.digitalObjectService.delete(digitalObject);
    log.info("Deleted object {} for project {}", digitalObject, project);
    String origin = ControllerUtils.resolveProxiedOrigin(requestHeader);
    return "redirect:" + origin + "api/v1/projects/" + project.getProjectAbbr() + "/objects";
  }

  @DeleteMapping
  public String deleteAllForProject(Project project, @RequestHeader Map<String, String> requestHeader){
    digitalObjectService.deleteAllForProject(project);
    log.info("Deleted all objects for project {}", project);
    String origin = ControllerUtils.resolveProxiedOrigin(requestHeader);
    return "redirect:" + origin + "api/v1/projects/" + project.getProjectAbbr() + "/objects";
  }

}
