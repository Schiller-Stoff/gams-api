package org.zim.gamsapi.Project;

import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.*;
import org.zim.gamsapi.DigitalObject.interfaces.DigitalObjectListItemView;
import org.zim.gamsapi.DigitalObject.interfaces.IDigitalObjectService;
import org.zim.gamsapi.Project.ProjectModification.IProjectModificationService;
import org.zim.gamsapi.Project.ProjectModification.ProjectModification;
import org.zim.gamsapi.Project.exceptions.ProjectException;
import org.zim.gamsapi.Project.interfaces.IProjectService;
import io.swagger.v3.oas.annotations.Operation;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@Controller
@RequestMapping({"/api/v1/projects", "/api/v1/projects/"})
public class ProjectController {

  private final IProjectService projectService;
  private final IProjectModificationService projectModificationService;

  private final IDigitalObjectService digitalObjectService;

  @Hidden
  @PutMapping(path = "/{projectAbbr}")
  public String createProject(Project project, Model model){
    projectService.save(project);;
    List<Project> projects = projectService.findAll();
    model.addAttribute("projects", projects);
    return "Project/show_all";
  }

  @Hidden
  @DeleteMapping(path = "/{projectAbbr}")
  @ResponseBody
  @Operation(summary = "Delete a project by abbreviation")
  public void deleteProject(@PathVariable String projectAbbr){
    Project project = projectService.findByAbbr(projectAbbr);
    log.info("Deleting project: " + project.getDescription());
    projectService.deleteProject(project);
  }

  @GetMapping
  @ResponseBody
  @Operation(summary = "A list of projects with metadata")
  public List<Project> showProjects(){
    return projectService.findAll();
  }

  @GetMapping(path = "/{projectAbbr}")
  @ResponseBody
  @Operation(summary = "A single project by proj́ect abbreviation and metadata")
  public Project getProjectByAbbr(@PathVariable String projectAbbr) {
    return projectService.findProject(projectAbbr);
  }

  @GetMapping(produces = MimeTypeUtils.TEXT_HTML_VALUE)
  public String showProjectsViaWebClient(Model model){
    List<Project> projects = projectService.findAll();
    model.addAttribute("projects", projects);
    return "Project/show_all";
  }

  @RequestMapping(value = "/{projectAbbr}", method = RequestMethod.HEAD)
  public ResponseEntity<Void> checkProjectModification(
      @PathVariable String projectAbbr,
      @RequestHeader(value = "If-Modified-Since") Optional<String> ifModifiedSince
  ) {

    // Get latest modification date across entire entity hierarchy
    ProjectModification projectModification = projectModificationService.
        findLatestModificationDate(projectAbbr);
    LocalDateTime lastModified = projectModification.getLastModificationDateAsLocalDateTime();
    // Format for HTTP header
    ZonedDateTime zonedDateTime = lastModified.atZone(ZoneId.systemDefault());

    // Handle conditional request
    if (ifModifiedSince.isPresent()) {
      String ifModifiedSinceHeaderValue = ifModifiedSince.get();
      try {
        ZonedDateTime ifModifiedSinceDate = ZonedDateTime.parse(
            ifModifiedSinceHeaderValue, DateTimeFormatter.RFC_1123_DATE_TIME);

        if (!zonedDateTime.isAfter(ifModifiedSinceDate)) {
          return ResponseEntity.status(HttpStatus.NOT_MODIFIED).build();
        }
      } catch (DateTimeParseException e) {
        String msg = String.format("Invalid date format for If-modified-since header: %s. Original error: %s", ifModifiedSince, e);
        log.error(msg);
        throw new ProjectException(HttpStatus.BAD_REQUEST, msg);
      }
    }

    return ResponseEntity.ok()
        .lastModified(zonedDateTime)
        .build();
  }

  @GetMapping(path = "/search/dc", produces = MimeTypeUtils.APPLICATION_JSON_VALUE)
  @ResponseBody
  @Operation(summary = "Dublin core search based on digital objects and project.")
  public Page<DigitalObjectListItemView> searchDigitalObjects(
      @RequestParam List<String> projectAbbrs,
      // dublin core search parameters
      @RequestParam String dcEntryName,
      @RequestParam String dcEntryValue,
      // for pagination
      @RequestParam(defaultValue = "0") int pageIndex,
      @RequestParam(defaultValue = "20") int pageSize
  ){

    // limit page size
    if (pageSize >= 20) {
      pageSize = 20;
    }

    return digitalObjectService.findDigitalObjectsByProjectAbbrsAndDublinCore(
        projectAbbrs, dcEntryName, dcEntryValue, PageRequest.of(pageIndex, pageSize)
    );
  }

}
