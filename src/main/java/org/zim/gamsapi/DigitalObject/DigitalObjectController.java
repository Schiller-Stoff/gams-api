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
import org.springframework.data.domain.Page;
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
import org.zim.gamsapi.DigitalObject.exceptions.DigitalObjectConversionException;
import org.zim.gamsapi.DigitalObject.interfaces.DigitalObjectDetailsView;
import org.zim.gamsapi.DigitalObject.interfaces.DigitalObjectListItemView;
import org.zim.gamsapi.Project.Project;
import org.zim.gamsapi.Project.ProjectBuilder;
import org.zim.gamsapi.Project.exceptions.ProjectException;
import org.zim.gamsapi.Project.interfaces.IProjectService;
import org.zim.gamsapi.System.utils.ControllerUtils;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequestMapping(value = { "/api/v1/projects/{projectAbbr}/objects" })
@Slf4j
@RequiredArgsConstructor
@Tag(name = "Digital Objects", description = "Digital object management operations")
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

  // @GetMapping(value = {"/{id}", "/{id}/"}, produces =
  // MimeTypeUtils.APPLICATION_JSON_VALUE)
  // @ResponseBody
  public DigitalObjectCompactDTO getObjectJson(DigitalObject digitalObject, Project project, Model model) {
    DigitalObjectDetailsView foundObject = digitalObjectService.findDigitalObjectDetailsViewById(digitalObject.getId());
    var datastreamDetailsViews = datastreamService.findAll(digitalObject);
    DigitalObjectCompactDTO digitalObjectCompactDTO = conversionService.convert(foundObject,
        DigitalObjectCompactDTO.class);

    if (digitalObjectCompactDTO == null) {
      String msg = String.format(
          "Failed to convert DigitalObjectDetailsView to DigitalObjectCompactDTO. For object %s for project %s",
          digitalObject, project);
      log.error(msg);
      throw new DigitalObjectConversionException(msg);
    }
    digitalObjectCompactDTO.setDatastreams(
        datastreamDetailsViews
            .stream()
            .map(
                IDatastreamDetailsView::getDsid)
            .collect(Collectors.toList()));

    model.addAttribute(digitalObjectCompactDTO);
    log.info("Found digital object {} for project {}", digitalObject, project);
    return digitalObjectCompactDTO;
  }

  @GetMapping(value = { "/{id}" }, produces = MimeTypeUtils.APPLICATION_JSON_VALUE)
  @ResponseBody
  @Operation(summary = "Get a digital object by its ID")
  @Parameter(name = "projectAbbr", description = "The project abbreviation", required = true)
  @Parameter(name = "id", description = "The digital object ID", required = true)
  public DigitalObjectCompactDTO getJson(@PathVariable String id) {
    return digitalObjectService.findDigitalObjectCompactDTOById(id);
  }

  @GetMapping(value = { "/{id}" }, produces = MimeTypeUtils.TEXT_HTML_VALUE)
  public String getObject(DigitalObject digitalObject, Project project, Model model) {
    // first query digital object projection dto
    DigitalObjectDetailsView foundObject = digitalObjectService.findDigitalObjectDetailsViewById(digitalObject.getId());
    DigitalObjectCompactDTO digitalObjectCompactDTO = conversionService.convert(foundObject,
        DigitalObjectCompactDTO.class);
    if (digitalObjectCompactDTO == null) {
      String msg = String.format(
          "Failed to convert DigitalObjectDetailsView to DigitalObjectCompactDTO. For object %s for project %s",
          digitalObject, project);
      log.error(msg);
      throw new DigitalObjectConversionException(msg);
    }

    // then query datastreams projections and assign to dto
    var datastreamDetailsViews = datastreamService.findAll(digitalObject);
    digitalObjectCompactDTO.setDatastreams(
        datastreamDetailsViews.stream().map(IDatastreamDetailsView::getDsid).collect(Collectors.toList()));

    model.addAttribute("do", digitalObjectCompactDTO);
    model.addAttribute(project);
    log.info("Found digital object {} for project {}", digitalObjectCompactDTO, project.getProjectAbbr());
    return "DigitalObject/show";
  }

  @GetMapping(produces = MimeTypeUtils.APPLICATION_JSON_VALUE)
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
  public List<DigitalObjectListItemView> getProjectObjectsJson(
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
        PageRequest.of(pageIndex, pageSize, Sort.by("id"))).toList();

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

    // Page<DigitalObject> digitalObjects =
    // digitalObjectService.findAllByProjectAbbr(project.getProjectAbbr(),
    // PageRequest.of(pageIndex, pageSize, Sort.by("id")));
    Page<DigitalObjectListItemView> digitalObjects = digitalObjectService.findAllByProjectAbbr(
        project.getProjectAbbr(),
        id,
        PageRequest.of(pageIndex, pageSize, Sort.by(sortBy)));

    // retrieve project info from database
    Project foundProject = projectService.findProject(project.getProjectAbbr());

    model.addAttribute("digitalObjects", digitalObjects.toList());
    model.addAttribute(foundProject);
    model.addAttribute("pageSize", pageSize);
    model.addAttribute("pageIndex", pageIndex);
    model.addAttribute("totalItems", digitalObjects.getTotalElements());
    model.addAttribute("totalPages", digitalObjects.getTotalPages());
    model.addAttribute("searchId", id);
    model.addAttribute("sortBy", sortBy);

    // log.info("Found objects {} for project {}", digitalObjects, project);
    return "DigitalObject/show_all";
  }

  @DeleteMapping(value = { "/{id}" })
  public String deleteObject(
      DigitalObject digitalObject,
      Project project,
      @RequestHeader Map<String, String> requestHeader) {
    this.digitalObjectService.delete(digitalObject);
    log.info("Deleted object {} for project {}", digitalObject, project);
    String origin = ControllerUtils.resolveProxiedOrigin(requestHeader);
    return "redirect:" + origin + "api/v1/projects/" + project.getProjectAbbr() + "/objects";
  }


  @Operation(
      summary = "Get all digital object IDs for a project",
      description = "Retrieves a list of all digital object IDs for a specific project"
  )
  @GetMapping(value = "/ids", produces = MimeTypeUtils.APPLICATION_JSON_VALUE)
  @ResponseBody
  public List<String> findAllIdsByProjectAbbr(@PathVariable String projectAbbr) {
    Project project = ProjectBuilder
        .builder()
        .projectAbbr(projectAbbr)
        .description("")
        .build();

    return digitalObjectService.findAllIdsByProjectAbbr(project.getProjectAbbr());
  }

}
