package org.ddh.gamsapi.domain.Project;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ddh.gamsapi.domain.Project.ProjectModification.IProjectModificationService;
import org.ddh.gamsapi.domain.Project.ProjectModification.ProjectModification;
import org.ddh.gamsapi.domain.Project.exceptions.ProjectInvalidDateFormatException;
import org.ddh.gamsapi.domain.Project.interfaces.IProjectService;
import org.ddh.gamsapi.infrastructure.System.config.OpenAPIConfig;
import org.ddh.gamsapi.infrastructure.System.dto.PagedResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@Controller
@RequestMapping({"/api/v1/projects" })
@Tag(name = OpenAPIConfig.PROJECTS_TAG, description = OpenAPIConfig.PROJECTS_TAG_DESCRIPTION)
public class ProjectController {

  private final IProjectService projectService;
  private final IProjectModificationService projectModificationService;

  @PatchMapping(path = "/{projectAbbr}", consumes = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  @Operation(
      summary = "Change a project's metadata",
      description = "Allows to change a project's metadata by providing the project abbreviation in the path variable and the new project data in the request body.",
      responses = {
          @ApiResponse(responseCode = "200", description = "Project updated successfully",
              content = @Content(mediaType = MimeTypeUtils.APPLICATION_JSON_VALUE)),
          @ApiResponse(responseCode = "404", description = "Project not found",
              content = @Content)
      }
  )
  public Project changeProject(
      @PathVariable String projectAbbr,
      @RequestBody Project project
  ){
    // project abbreviation is set via path variable and not via json
    project.setProjectAbbr(projectAbbr);
    return projectService.updateProject(project);
  }

  @Hidden
  @PatchMapping(path = "/{projectAbbr}", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
  public String changeProjectFromForm(
      @PathVariable String projectAbbr,
      @RequestParam String description
  ) {
    Project project = ProjectBuilder.builder()
        .projectAbbr(projectAbbr)
        .description(description)
        .build();
    projectService.updateProject(project);
    return "redirect:/api/v1/projects";
  }

  @PutMapping(path = "/{projectAbbr}")
  @ResponseBody
  @Operation(
      summary = "Create a GAMS project",
      description = "Allows to create a GAMS project by providing the project abbreviation in the path variable and the project data in the request body.",
      responses = {
          @ApiResponse(responseCode = "200", description = "Project successfully created",
              content = @Content)
      }
  )
  public void createProject(
      @PathVariable String projectAbbr,
      // read out description argument from given json
      @RequestBody Optional<Project> projectToBeSaved
  ){

    projectToBeSaved.ifPresentOrElse(
        // if given responseBody is available, save it
        project -> {
          project.setProjectAbbr(projectAbbr);
          projectService.save(project);
        }, () -> {
          // if no responseBody is given, create a new project instance with the given projectAbbr
          projectService.save(
              ProjectBuilder
                  .builder()
                  .projectAbbr(projectAbbr)
                  .build()
          );
    });

  }

  /**
   * Creates a project from a Thymeleaf form submission.
   * Redirects back to the projects overview after creation.
   */
  @Hidden
  @PutMapping(path = "/{projectAbbr}", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
  public String createProjectFromForm(
      @PathVariable String projectAbbr,
      @RequestParam(required = false) String description
  ) {
    Project project = ProjectBuilder.builder()
        .projectAbbr(projectAbbr)
        .description(description)
        .build();
    projectService.save(project);
    return "redirect:/api/v1/projects";
  }

  /**
   * Deletes a project and redirects back to the projects overview.
   *
   */
  @Hidden
  @DeleteMapping(path = "/{projectAbbr}", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
  public String deleteProjectFromForm(@PathVariable String projectAbbr) {
    Project project = projectService.findByAbbr(projectAbbr);
    projectService.deleteProject(project);
    return "redirect:/api/v1/projects";
  }

  @DeleteMapping(path = "/{projectAbbr}")
  @ResponseBody
  @Operation(
      summary = "Delete a project by abbreviation",
      description = "Deletes a project by its abbreviation. The project must exist.",
      responses = {
          @ApiResponse(responseCode = "204", description = "Project deleted successfully",
              content = @Content),
          @ApiResponse(responseCode = "404", description = "Project not found",
              content = @Content)
      }
  )
  public void deleteProject(@PathVariable String projectAbbr){
    Project project = projectService.findByAbbr(projectAbbr);
    projectService.deleteProject(project);
  }

  @GetMapping
  @ResponseBody
  @Operation(
      summary = "A list of projects with metadata",
      description = "Returns a list of all projects with their metadata.",
      responses = {
          @ApiResponse(responseCode = "200", description = "List of projects",
              content = @Content(mediaType = MimeTypeUtils.APPLICATION_JSON_VALUE)),
      }
  )
  public PagedResponse<Project> showProjects(
      @RequestParam(defaultValue = "0") int pageIndex,
      @RequestParam(defaultValue = "100") int pageSize,
      @RequestParam(defaultValue = "projectAbbr") String sortBy
  ){
    // limit pageSize to max 100
    if (pageSize >= 100) {
      pageSize = 100;
    }

    return projectService.findAllPaged(
        PageRequest.of(pageIndex, pageSize, Sort.by(sortBy))
    );
  }

  @GetMapping(path = "/{projectAbbr}")
  @ResponseBody
  @Operation(
      summary = "A single project by proj́ect abbreviation and metadata",
      description = "Returns a single project by its abbreviation with all metadata.",
      responses = {
          @ApiResponse(responseCode = "200", description = "Project found",
              content = @Content(mediaType = MimeTypeUtils.APPLICATION_JSON_VALUE)),
          @ApiResponse(responseCode = "404", description = "Project not found",
              content = @Content)
      }
  )
  public Project getProjectByAbbr(@PathVariable String projectAbbr) {
    return projectService.findProject(projectAbbr);
  }

  @Operation(hidden = true)
  @GetMapping(produces = MimeTypeUtils.TEXT_HTML_VALUE)
  public String showProjectsViaWebClient(
      Model model,
      // for pagination
      @RequestParam(defaultValue = "0") int pageIndex,
      @RequestParam(defaultValue = "10") int pageSize,
      @RequestParam(defaultValue = "projectAbbr") String sortBy
  ){
    var projects = projectService.findAllPaged(
        PageRequest.of(pageIndex, pageSize, Sort.by(sortBy))
    );
    model.addAttribute("projects", projects.getContent());
    model.addAttribute("pageSize", pageSize);
    model.addAttribute("pageIndex", pageIndex);
    model.addAttribute("totalItems", projects.getPagination().getTotalElements());
    model.addAttribute("totalPages", projects.getPagination().getTotalPages());
    model.addAttribute("sortBy", sortBy);

    return "Project/show_all";
  }

  @Operation(
      summary = "Check if the project's metadata has been modified since a given date",
      description = "Checks if the project's metadata has been modified since a given date (E.g. the project description). Changes to sub resources like digital objects and datastreams are not reflected in this modification date. If the project has not been modified, it returns a 304 Not Modified status.",
      responses = {
          @ApiResponse(responseCode = "200", description = "Project has been modified",
              content = @Content),
          @ApiResponse(responseCode = "304", description = "Project has not been modified",
              content = @Content),
          @ApiResponse(responseCode = "400", description = "Invalid date format for If-modified-since header",
              content = @Content)
      }
  )
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
        throw new ProjectInvalidDateFormatException(
            "Invalid date format for If-modified-since header: " + ifModifiedSince + ". Original error: " + e.getMessage(),
            e
        );
      }
    }

    return ResponseEntity.ok()
        .lastModified(zonedDateTime)
        .build();
  }

  @Operation(
      summary = "Check if the project's sub resources have been modified since a given date",
      description = "Checks if the project's sub resources have been modified since a given date (E.g. digital objects and datastreams). Changes to the project metadata itself (project description or abbreviation) are not reflected in this modification date. If the project's content have not been modified, it returns a 304 Not Modified status.",
      responses = {
          @ApiResponse(responseCode = "200", description = "Project has been modified",
              content = @Content),
          @ApiResponse(responseCode = "304", description = "Project has not been modified",
              content = @Content),
          @ApiResponse(responseCode = "400", description = "Invalid date format for If-modified-since header",
              content = @Content)
      }
  )
  @RequestMapping(value = "/{projectAbbr}/objects", method = RequestMethod.HEAD)
  public ResponseEntity<Void> checkProjectContentModification(
      @PathVariable String projectAbbr,
      @RequestHeader(value = "If-Modified-Since") Optional<String> ifModifiedSince
  ) {

    // Get latest content modification date
    ProjectModification projectModification = projectModificationService.
        findContentLatestModificationDate(projectAbbr);
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
        throw new ProjectInvalidDateFormatException(
            "Invalid date format for If-modified-since header: " + ifModifiedSince + ". Original error: " + e.getMessage(),
            e
        );
      }
    }

    return ResponseEntity.ok()
        .lastModified(zonedDateTime)
        .build();
  }


}
