package org.zim.gamsapi.domain.DigitalObject;

import io.micrometer.common.lang.Nullable;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.*;
import org.zim.gamsapi.domain.Datastream.DatastreamService;
import org.zim.gamsapi.domain.Datastream.utils.interfaces.IDatastreamDetailsView;
import org.zim.gamsapi.domain.DigitalObject.DigitalObjectModification.DigitalObjectModification;
import org.zim.gamsapi.domain.DigitalObject.DigitalObjectModification.IDigitalObjectModificationService;
import org.zim.gamsapi.domain.DigitalObject.SubmissionRecord.ISubmissionRecordService;
import org.zim.gamsapi.domain.DigitalObject.utils.dto.DigitalObjectCompactDTO;
import org.zim.gamsapi.domain.DigitalObject.utils.interfaces.DigitalObjectListItemView;
import org.zim.gamsapi.domain.Project.Project;
import org.zim.gamsapi.domain.Project.ProjectBuilder;
import org.zim.gamsapi.domain.Project.exceptions.ProjectException;
import org.zim.gamsapi.domain.Project.interfaces.IProjectService;
import org.zim.gamsapi.infrastructure.System.config.OpenAPIConfig;
import org.zim.gamsapi.infrastructure.System.dto.PagedResponse;
import org.zim.gamsapi.infrastructure.System.utils.ControllerUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Controller
@RequestMapping(value = { "/api/v1/projects/{projectAbbr}/objects" })
@Slf4j
@RequiredArgsConstructor
@Tag(name = OpenAPIConfig.DIGITAL_OBJECTS_TAG, description = OpenAPIConfig.DIGITAL_OBJECTS_TAG_DESCRIPTION)
public class DigitalObjectController {

  private final DigitalObjectService digitalObjectService;
  private final DatastreamService datastreamService;
  private final IProjectService projectService;
  private final IDigitalObjectModificationService digitalObjectModificationService;
  private final ISubmissionRecordService submissionRecordService;


  @Operation(
      summary = "Check if the digital object's sub resources have been modified since a given date",
      description = "Checks if the digital object's sub resources have been modified since given date (datastreams). Changes to the object's itself (id etc.) are not reflected in this modification date. If the object's content have not been modified, it returns a 304 Not Modified status.",
      responses = {
          @ApiResponse(responseCode = "200", description = "Digital object sub resources have been modified",
              content = @Content),
          @ApiResponse(responseCode = "304", description = "Digital object sub resources have not been modified",
              content = @Content),
          @ApiResponse(responseCode = "400", description = "Invalid date format for If-modified-since header",
              content = @Content)
      }
  )
  @RequestMapping(value = "/{id}/datastreams", method = RequestMethod.HEAD)
  public ResponseEntity<Void> checkDigitalObjectContentModification(
      @PathVariable String projectAbbr,
      @PathVariable String id,
      @RequestHeader(value = "If-Modified-Since") Optional<String> ifModifiedSince
  ) {

    // Get latest modification date across entire entity hierarchy
    DigitalObjectModification digitalObjectModification = digitalObjectModificationService.
        findLatestModificationDate(projectAbbr, id);

    LocalDateTime lastModified = digitalObjectModification.getLastModificationDateAsLocalDateTime();

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

  @Operation(
      summary = "Check if the digital object's metadata has been modified since a given date",
      description = "Checks if the digital object's metadata has been modified since given date (datastreams). If the object's metadata has not been modified, it returns a 304 Not Modified status.",
      responses = {
          @ApiResponse(responseCode = "200", description = "Digital object's metadata has been modified",
              content = @Content),
          @ApiResponse(responseCode = "304", description = "Digital object's metadata has not been modified",
              content = @Content),
          @ApiResponse(responseCode = "400", description = "Invalid date format for If-modified-since header",
              content = @Content)
      }
  )
  @RequestMapping(value = "/{id}", method = RequestMethod.HEAD)
  public ResponseEntity<Void> checkDigitalObjectModification(
      @PathVariable String projectAbbr,
      @PathVariable String id,
      @RequestHeader(value = "If-Modified-Since") Optional<String> ifModifiedSince
  ) {

    // Get latest modification date across entire entity hierarchy
    DigitalObjectModification digitalObjectModification = digitalObjectModificationService.
        findLastModifiedDate(projectAbbr, id);

    LocalDateTime lastModified = digitalObjectModification.getLastModificationDateAsLocalDateTime();

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

  @GetMapping(value = { "/{id}" }, produces = {
      MimeTypeUtils.APPLICATION_JSON_VALUE,
      MimeTypeUtils.APPLICATION_XML_VALUE
  })
  @ResponseBody
  @Operation(summary = "Get a digital object by its ID")
  @Parameter(name = "projectAbbr", description = "The project abbreviation", required = true)
  @Parameter(name = "id", description = "The digital object ID", required = true)
  public DigitalObjectCompactDTO getJson(@PathVariable String id) {
    return digitalObjectService.findDigitalObjectCompactDTOById(id);
  }

  @Hidden
  @GetMapping(value = { "/{id}" }, produces = MimeTypeUtils.TEXT_HTML_VALUE)
  public String getObject(
      DigitalObject digitalObject,
      Project project,
      Model model,
      @RequestParam(defaultValue = "0") int pageIndex,
      @RequestParam(defaultValue = "10") int pageSize,
      @RequestParam(defaultValue = "dsid") String sortBy,
      @RequestParam(defaultValue = "asc") String sortDir,
      @RequestParam(defaultValue = "") String searchDsid
  ) {

    if (pageSize >= 100) {
      pageSize = 100;
    }

    // first query digital object projection dto
    var foundObject = digitalObjectService.findDigitalObjectCompactDTOById(digitalObject.getId());

    var submissionRecord = submissionRecordService.find(digitalObject.getId());
    model.addAttribute("submissionRecord", submissionRecord);

    // TODO atm loading a lot of data, maybe we should use a different projection here? e.g. DatastreamMimeView?
    PagedResponse<IDatastreamDetailsView> pagedDatastreams = datastreamService.findAll(
        foundObject.getId(), PageRequest.of(pageIndex, pageSize, Sort.by(sortBy))
    );
    model.addAttribute("pageSize", pageSize);
    model.addAttribute("pageIndex", pageIndex);
    model.addAttribute("sortDir", sortDir);
    model.addAttribute("sortBy", sortBy);
    model.addAttribute("searchDsid", searchDsid);
    model.addAttribute("pagedDatastreams", pagedDatastreams);
    model.addAttribute("do", foundObject);
    model.addAttribute(project);
    log.info("Found digital object {} for project {}", foundObject, project.getProjectAbbr());
    return "DigitalObject/show";
  }

  @GetMapping(produces = {
      MimeTypeUtils.APPLICATION_JSON_VALUE,
      MimeTypeUtils.APPLICATION_XML_VALUE
  })
  @ResponseBody
  @Operation(
      summary = "Get digital objects for a project",
      description = "Retrieves paginated list of digital objects for a specific project with optional filtering",
      responses = {
          @ApiResponse(responseCode = "200", description = "Successfully retrieved digital objects"),
          @ApiResponse(responseCode = "404", description = "Project not found", content = @Content),
          @ApiResponse(responseCode = "403", description = "Access denied to project", content = @Content)
      }
  )
  public PagedResponse<DigitalObjectListItemView> getProjectObjectsJson(
      @PathVariable String projectAbbr,
      Model model,
      // for pagination
      @RequestParam(defaultValue = "0") int pageIndex,
      @RequestParam(defaultValue = "20") int pageSize,
      // optional parameters searching for explicit types?
      @Nullable @RequestParam Optional<String> objectType,
      @RequestParam Optional<Set<String>> types) {
    // limit page size
    if (pageSize >= 20) {
      pageSize = 20;
    }

    Project project = ProjectBuilder.builder()
        .projectAbbr(projectAbbr)
        .description("")
        .build();

    model.addAttribute(project);

    return digitalObjectService.findAllByProjectAbbr(
        project.getProjectAbbr(),
        objectType,
        PageRequest.of(pageIndex, pageSize, Sort.by("id")));

  }

  @GetMapping(produces = MimeTypeUtils.TEXT_HTML_VALUE)
  public String getProjectObjects(
      Model model,
      Project project,
      // for pagination
      @RequestParam(defaultValue = "0") int pageIndex,
      @RequestParam(defaultValue = "25") int pageSize,
      @RequestParam(defaultValue = "") String id,
      @RequestParam(defaultValue = "id") String sortBy

  ) {
    // limit pageSize to max 100
    if (pageSize >= 100) {
      pageSize = 100;
    }

    var digitalObjects = digitalObjectService.findAllByProjectAbbr(
        project.getProjectAbbr(),
        id,
        PageRequest.of(pageIndex, pageSize, Sort.by(sortBy)));

    // retrieve project info from database
    Project foundProject = projectService.findProject(project.getProjectAbbr());

    model.addAttribute("digitalObjects", digitalObjects.getContent());
    model.addAttribute(foundProject);
    model.addAttribute("pageSize", pageSize);
    model.addAttribute("pageIndex", pageIndex);
    model.addAttribute("totalItems", digitalObjects.getPagination().getTotalElements());
    model.addAttribute("totalPages", digitalObjects.getPagination().getTotalPages());
    model.addAttribute("searchId", id);
    model.addAttribute("sortBy", sortBy);

    // log.info("Found objects {} for project {}", digitalObjects, project);
    return "DigitalObject/show_all";
  }

  @DeleteMapping(value = { "/{id}" })
  @Operation(summary = "Delete a digital object by its ID",
      description = "Deletes a digital object from the specified project. This operation is irreversible.")
  public String deleteObject(
      @PathVariable String id,
      @PathVariable String projectAbbr,
      @RequestHeader Map<String, String> requestHeader) {

    DigitalObject digitalObject = DigitalObjectBuilder
        .builder()
        .id(id)
        .project(projectAbbr)
        .publisher("_")
        .build();

    this.digitalObjectService.delete(digitalObject);
    log.info("Deleted object {} for project {}", digitalObject, projectAbbr);
    String origin = ControllerUtils.resolveProxiedOrigin(requestHeader);
    return "redirect:" + origin + "api/v1/projects/" + projectAbbr + "/objects";
  }


  @Operation(
      summary = "Get all digital object IDs for a project",
      description = "Retrieves a list of all digital object IDs for a specific project"
  )
  @GetMapping(value = "/ids", produces = {
      MimeTypeUtils.APPLICATION_JSON_VALUE,
      MimeTypeUtils.APPLICATION_XML_VALUE
  })
  @ResponseBody
  public PagedResponse<String> findAllIdsByProjectAbbr(
      @PathVariable String projectAbbr,
      @RequestParam(defaultValue = "0") int pageIndex,
      @RequestParam(defaultValue = "10000") int pageSize,
      @RequestParam(defaultValue = "id") String sortBy
  ) {
    // limit pageSize
    if (pageSize >= 10000) {
      pageSize = 10000;
    }

    Project project = ProjectBuilder
        .builder()
        .projectAbbr(projectAbbr)
        .description("")
        .build();

    return digitalObjectService.findAllIdsByProjectAbbr(
        project.getProjectAbbr(),
        PageRequest.of(pageIndex, pageSize, Sort.by(sortBy))
    );
  }




}
