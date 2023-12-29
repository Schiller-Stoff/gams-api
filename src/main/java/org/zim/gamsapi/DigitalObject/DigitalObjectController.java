package org.zim.gamsapi.DigitalObject;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.*;
import org.zim.gamsapi.DigitalObject.interfaces.DigitalObjectListItemView;
import org.zim.gamsapi.Project.Project;
import org.zim.gamsapi.Project.interfaces.IProjectService;
import org.zim.gamsapi.System.utils.ControllerUtils;

import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping(value = {"/api/v1/projects/{projectAbbr}/objects", "/api/v1/projects/{projectAbbr}/objects/"})
@Slf4j
@RequiredArgsConstructor
public class DigitalObjectController {

  private final DigitalObjectService digitalObjectService;
  private final IProjectService projectService;

  @GetMapping(value = {"/{id}", "/{id}/"}, produces = MimeTypeUtils.APPLICATION_JSON_VALUE)
  @ResponseBody
  public DigitalObject getObjectJson(DigitalObject digitalObject, Project project, Model model) {
    DigitalObject foundObject = digitalObjectService.findById(digitalObject.getId());
    model.addAttribute(foundObject);
    log.info("Found digital object {} for project {}", digitalObject, project);
    return foundObject;
  }

  @GetMapping(value = {"/{id}", "/{id}/"}, produces = MimeTypeUtils.TEXT_HTML_VALUE)
  public String getObject(DigitalObject digitalObject, Project project, Model model) {
    DigitalObject foundDigitalObject = digitalObjectService.findById(digitalObject.getId());
    model.addAttribute("do", foundDigitalObject);
    model.addAttribute(project);
    log.info("Found digital object {} for project {}", foundDigitalObject, project.getProjectAbbr());
    return "DigitalObject/show";
  }


  @GetMapping(produces = MimeTypeUtils.TEXT_HTML_VALUE)
  public String getProjectObjects(
          Model model,
          Project project,
          // for pagination
          @RequestParam(defaultValue = "0") int pageIndex,
          @RequestParam(defaultValue = "100") int pageSize,
          @RequestParam(defaultValue = "") String id,
          @RequestParam(defaultValue = "id") String sortBy

  ) {
    // limit pageSize to max 100
    if (pageSize >= 100) {
      pageSize = 100;
    }

    //Page<DigitalObject> digitalObjects = digitalObjectService.findAllByProjectAbbr(project.getProjectAbbr(), PageRequest.of(pageIndex, pageSize, Sort.by("id")));
    Page<DigitalObjectListItemView> digitalObjects = digitalObjectService.findAllByProjectAbbr(
            project.getProjectAbbr(),
            id,
            PageRequest.of(pageIndex, pageSize, Sort.by(sortBy))
    );

    model.addAttribute("digitalObjects", digitalObjects.toList());
    model.addAttribute(project);
    model.addAttribute("pageSize", pageSize);
    model.addAttribute("pageIndex", pageIndex);
    model.addAttribute("totalItems", digitalObjects.getTotalElements());
    model.addAttribute("totalPages", digitalObjects.getTotalPages());
    model.addAttribute("searchId", id);
    model.addAttribute("sortBy", sortBy);

    //log.info("Found objects {} for project {}", digitalObjects, project);
    return "DigitalObject/show_all";
  }

  @GetMapping(produces = MimeTypeUtils.APPLICATION_JSON_VALUE)
  @ResponseBody
  public List<DigitalObjectListItemView> getProjectObjectsJson(
          Model model,
          Project project,
          // for pagination
          @RequestParam(defaultValue = "0") int pageIndex,
          @RequestParam(defaultValue = "15") int pageSize,
          // optional parameters searching for explicit types?
          @RequestParam Optional<String> objectType,
          @RequestParam Optional<Set<String>> types
  ) {
    // limit page size
    if (pageSize >= 20) {
      pageSize = 20;
    }

    model.addAttribute(project);

    return digitalObjectService.findAllByProjectAbbr(
            project.getProjectAbbr(),
            objectType,
            types,
            PageRequest.of(pageIndex, pageSize, Sort.by("id"))
    ).toList();

  }

  /**
   * Allows to assign child objects to given parent object
   * @param id the id of the parent object
   * @param childObjects the child objects ids
   * @param project the project
   * @return the parent object with the assigned child objects
   */
  @PatchMapping(produces = MimeTypeUtils.APPLICATION_JSON_VALUE, value = {"/{id}/collect", "/{id}/collect/"})
  @ResponseBody
  public DigitalObject collectObjects(
          @PathVariable String id,
          @RequestParam Set<String> childObjects,
          Project project
  ) {

    Set<DigitalObject> childrenToBeCollected = childObjects
            .stream()
            .map(childId -> DigitalObject.builder().id(childId).project(project).build())
            .collect(Collectors.toSet());

    DigitalObject foundObject = digitalObjectService.assignChildObjects(
            DigitalObject.builder().id(id).project(project).build(), childrenToBeCollected
    );

    return foundObject;
  }

  @PutMapping(value = {"/{id}", "/{id}/"})
  public String createObject(
          DigitalObject digitalObject,
          @RequestParam Optional<Set<String>> childObjects,
          Project project,
          Model model,
          @RequestHeader Map<String, String> requestHeader
  ) {
    // project membership is not automatically bound by spring.
    digitalObject.setProject(project);
    // assign child objects if available
    childObjects
      .ifPresent(
        strings -> digitalObject.setChildObjects(strings.stream().map(id -> DigitalObject.builder().id(id).build()).collect(Collectors.toSet()))
      );

    DigitalObject savedObject = digitalObjectService.save(digitalObject);
    model.addAttribute("do", savedObject);
    log.info("Created object {} for project {}", savedObject, project);

    // needed to consider proxy forwarding
    String origin = ControllerUtils.resolveProxiedOrigin(requestHeader);
    return "redirect:" + origin + "api/v1/projects/" + project.getProjectAbbr() + "/objects/" + savedObject.getId();
  }

  @DeleteMapping(value = {"/{id}", "/{id}/"})
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
