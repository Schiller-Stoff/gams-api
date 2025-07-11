package org.zim.gamsapi.DigitalObject;

import io.micrometer.common.lang.Nullable;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.*;
import org.zim.gamsapi.Datastream.DatastreamService;
import org.zim.gamsapi.Datastream.interfaces.IDatastreamDetailsView;
import org.zim.gamsapi.DigitalObject.DigitalObjectModification.DigitalObjectModification;
import org.zim.gamsapi.DigitalObject.DigitalObjectModification.IDigitalObjectModificationService;
import org.zim.gamsapi.DigitalObject.dto.DigitalObjectCompactDTO;
import org.zim.gamsapi.DigitalObject.exceptions.DigitalObjectConversionException;
import org.zim.gamsapi.DigitalObject.interfaces.DigitalObjectDetailsView;
import org.zim.gamsapi.DigitalObject.interfaces.DigitalObjectListItemView;
import org.zim.gamsapi.Project.Project;
import org.zim.gamsapi.Project.ProjectBuilder;
import org.zim.gamsapi.Project.exceptions.ProjectException;
import org.zim.gamsapi.Project.interfaces.IProjectService;
import org.zim.gamsapi.System.config.OpenAPIConfig;
import org.zim.gamsapi.System.dto.PagedResponse;
import org.zim.gamsapi.System.utils.ControllerUtils;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequestMapping(value = { "/api/v1/projects/{projectAbbr}/objects" })
@Slf4j
@RequiredArgsConstructor
@Tag(name = OpenAPIConfig.DIGITAL_OBJECTS_TAG, description = OpenAPIConfig.DIGITAL_OBJECTS_TAG_DESCRIPTION)
public class DigitalObjectController {

  private final DigitalObjectService digitalObjectService;
  private final DatastreamService datastreamService;
  private final IProjectService projectService;
  private final ConversionService conversionService;
  private final IDigitalObjectModificationService digitalObjectModificationService;


  @RequestMapping(value = "/{id}", method = RequestMethod.HEAD)
  public ResponseEntity<Void> checkDigitalObjectModification(
      @PathVariable String id,
      @RequestHeader(value = "If-Modified-Since") Optional<String> ifModifiedSince
  ) {

    // Get latest modification date across entire entity hierarchy
    DigitalObjectModification digitalObjectModification = digitalObjectModificationService.
        findLatestModificationDate(id);

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

    // TODO pagination max size etc.
    if (pageSize >= 100) {
      pageSize = 100;
    }

    // first query digital object projection dto
    var foundObject = digitalObjectService.findDigitalObjectCompactDTOById(digitalObject.getId());

    // TODO I do not understand why the next lines should be needed.
    DigitalObjectCompactDTO digitalObjectCompactDTO = conversionService.convert(foundObject,
        DigitalObjectCompactDTO.class);
    if (digitalObjectCompactDTO == null) {
      String msg = String.format(
          "Failed to convert DigitalObjectDetailsView to DigitalObjectCompactDTO. For object %s for project %s",
          digitalObject, project);
      log.error(msg);
      throw new DigitalObjectConversionException(msg);
    }

    PagedResponse<IDatastreamDetailsView> pagedDatastreams = datastreamService.findAll(
        foundObject.getId(), PageRequest.of(pageIndex, pageSize, Sort.by(sortBy))
    );
    // TODO add sorting params
    model.addAttribute("pageSize", pageSize);
    model.addAttribute("pageIndex", pageIndex);
    model.addAttribute("sortDir", sortDir);
    model.addAttribute("sortBy", sortBy);
    model.addAttribute("searchDsid", searchDsid);

    model.addAttribute("pagedDatastreams", pagedDatastreams);
    model.addAttribute("do", digitalObjectCompactDTO);
    model.addAttribute(project);
    log.info("Found digital object {} for project {}", digitalObjectCompactDTO, project.getProjectAbbr());
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

    // TODO should return a paginated response
    return digitalObjectService.findAllIdsByProjectAbbr(
        project.getProjectAbbr(),
        PageRequest.of(pageIndex, pageSize, Sort.by(sortBy))
    );
  }

}
