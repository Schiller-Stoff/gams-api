package org.ddh.gamsapi.domain.DigitalObject;

import io.micrometer.common.lang.Nullable;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ddh.gamsapi.domain.Datastream.DatastreamService;
import org.ddh.gamsapi.domain.Datastream.utils.interfaces.IDatastreamDetailsView;
import org.ddh.gamsapi.domain.DigitalObject.ArchivalRecord.IArchivalRecordService;
import org.ddh.gamsapi.domain.DigitalObject.DigitalObjectModification.DigitalObjectModification;
import org.ddh.gamsapi.domain.DigitalObject.DigitalObjectModification.IDigitalObjectModificationService;
import org.ddh.gamsapi.domain.DigitalObject.SubmissionRecord.ISubmissionRecordService;
import org.ddh.gamsapi.domain.DigitalObject.utils.dto.DigitalObjectCompactDTO;
import org.ddh.gamsapi.domain.DigitalObject.utils.dto.DigitalObjectCreateDto;
import org.ddh.gamsapi.domain.DigitalObject.utils.dto.DigitalObjectUpdateDto;
import org.ddh.gamsapi.domain.DigitalObject.utils.exceptions.DigitalObjectInvalidDateFormatException;
import org.ddh.gamsapi.domain.DigitalObject.utils.interfaces.DigitalObjectListItemView;
import org.ddh.gamsapi.domain.Project.Project;
import org.ddh.gamsapi.domain.Project.ProjectBuilder;
import org.ddh.gamsapi.domain.Project.interfaces.IProjectService;
import org.ddh.gamsapi.infrastructure.System.config.OpenAPIConfig;
import org.ddh.gamsapi.infrastructure.System.dto.PagedResponse;
import org.ddh.gamsapi.infrastructure.System.utils.ControllerUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequestMapping(value = { "/api/curation/v1/projects/{projectAbbr}/objects" })
@Slf4j
@RequiredArgsConstructor
@Tag(name = OpenAPIConfig.DIGITAL_OBJECTS_TAG, description = OpenAPIConfig.DIGITAL_OBJECTS_TAG_DESCRIPTION)
public class DigitalObjectController {

  // TODO inject interfaces instead of implementations
  private final DigitalObjectService digitalObjectService;
  private final DatastreamService datastreamService;
  private final IProjectService projectService;
  private final IDigitalObjectModificationService digitalObjectModificationService;
  private final ISubmissionRecordService submissionRecordService;
  private final IArchivalRecordService archivalRecordService;


  @Operation(
      summary = "Check if a digital object has been modified since a given date",
      description = "Checks if the digital object has been modified since given date (datastreams). If the object has not been modified, it returns a 304 Not Modified status.",
      responses = {
          @ApiResponse(responseCode = "200", description = "Digital object has been modified",
              content = @Content),
          @ApiResponse(responseCode = "304", description = "Digital object has not been modified",
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
        throw new DigitalObjectInvalidDateFormatException(
            "Invalid date format for If-Modified-Since header: " + ifModifiedSinceHeaderValue + ". Original error: " + e.getMessage(),
            e
        );
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
      Authentication authentication,
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

    submissionRecordService.find(digitalObject.getId()).ifPresent(submissionRecord -> {
      model.addAttribute("submissionRecord", submissionRecord);
    });

    var archivalRecords = archivalRecordService.findForObject(digitalObject.getId());
    model.addAttribute("archivalRecords", archivalRecords);

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

    // TODO can this be done via global controller advice?
    boolean canEdit = authentication != null && authentication.isAuthenticated()
        && !(authentication instanceof AnonymousAuthenticationToken);
    model.addAttribute("isAuthenticated", canEdit);

    // tags of object sorted
    model.addAttribute("sortedTagsCsv",
        foundObject.getTags().stream().sorted().collect(Collectors.joining(", ")));

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
      // for pagination
      @RequestParam(defaultValue = "0") int pageIndex,
      @RequestParam(defaultValue = "20") int pageSize,
      @RequestParam(defaultValue = "id") String sortBy,
      // optional parameters searching for explicit types?
      @Nullable @RequestParam Optional<String> objectType,
      @RequestParam Optional<Set<String>> types,
      @RequestParam(required = false, name = "tag", defaultValue = "") Set<String> tags
      ) {

    // TODO add sort direction?

    // limit page size
    if (pageSize >= 20) {
      pageSize = 20;
    }

    Project project = ProjectBuilder.builder()
        .projectAbbr(projectAbbr)
        .description("")
        .build();

    PagedResponse<DigitalObjectListItemView> digitalObjects;
    if(tags.isEmpty()){
      digitalObjects = digitalObjectService.findAllByProjectAbbr(
          project.getProjectAbbr(),
          objectType,
          PageRequest.of(pageIndex, pageSize, Sort.by("id"))
      );
    } else {
      digitalObjects = digitalObjectService.findAllByProjectAndTags(
          project.getProjectAbbr(),
          tags,
          PageRequest.of(pageIndex, pageSize, Sort.by(sortBy)));
    }

    return digitalObjects;

  }

  @GetMapping(produces = MimeTypeUtils.TEXT_HTML_VALUE)
  public String getProjectObjects(
      Model model,
      Project project,
      // for pagination
      @RequestParam(defaultValue = "0") int pageIndex,
      @RequestParam(defaultValue = "25") int pageSize,
      @RequestParam(defaultValue = "") String id,
      @RequestParam(required = false, name = "tag", defaultValue = "") Set<String> tags,
      @RequestParam(defaultValue = "id") String sortBy

  ) {
    // TODO add sort direction?

    // limit pageSize to max 100
    if (pageSize >= 100) {
      pageSize = 100;
    }

    PagedResponse<DigitalObjectListItemView> digitalObjects;
    if(tags.isEmpty()){
      digitalObjects = digitalObjectService.findAllByProjectAbbr(
          project.getProjectAbbr(),
          id,
          PageRequest.of(pageIndex, pageSize, Sort.by(sortBy)));
    } else {
      digitalObjects = digitalObjectService.findAllByProjectAndTags(
          project.getProjectAbbr(),
          tags,
          PageRequest.of(pageIndex, pageSize, Sort.by(sortBy)));
    }

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

    return "DigitalObject/show_all";
  }

  @DeleteMapping(value = { "/{id}" }, consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
  @Hidden
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
    String origin = ControllerUtils.resolveProxiedOrigin(requestHeader);
    return "redirect:" + origin + "api/curation/v1/projects/" + projectAbbr + "/objects";
  }

  @DeleteMapping(value = { "/{id}" })
  @Operation(summary = "Delete a digital object by its ID",
      description = "Deletes a digital object from the specified project. This operation is irreversible.")
  @ResponseBody
  public void deleteObjectJson(
      @PathVariable String id,
      @PathVariable String projectAbbr) {

    DigitalObject digitalObject = DigitalObjectBuilder
        .builder()
        .id(id)
        .project(projectAbbr)
        .publisher("_")
        .build();

    this.digitalObjectService.delete(digitalObject);
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

  @GetMapping(path = "/tags", produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  @Operation(
      summary = "Get all unique tags used in project",
      description = "Returns a list of all distinct tags used by digital objects in the project. " +
          "Useful for tag filters, autocomplete, or tag clouds."
  )
  public Set<String> getProjectTags(@PathVariable String projectAbbr) {
    return digitalObjectService.findDistinctTagsByProject(projectAbbr);
  }


  // In DigitalObjectController.java

  @Hidden // hide from OpenAPI — this is webclient-only
  @PostMapping(consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
  public String createObjectFromForm(
      @PathVariable String projectAbbr,
      @Valid DigitalObjectCreateDto dto,
      @RequestHeader Map<String, String> requestHeader
  ) {

    digitalObjectService.create(projectAbbr, dto);
    String origin = ControllerUtils.resolveProxiedOrigin(requestHeader);
    return "redirect:" + origin + "api/curation/v1/projects/" + projectAbbr + "/objects";
  }


  @PatchMapping(
      value = "/{id}",
      consumes = MimeTypeUtils.APPLICATION_JSON_VALUE,
      produces = MimeTypeUtils.APPLICATION_JSON_VALUE
  )
  @ResponseBody
  @Operation(
      summary = "Update a digital object's metadata",
      description = "Partially updates a digital object's metadata. Only fields present in the "
          + "request body are updated; omitted fields remain unchanged. Fields like title, rights, "
          + "creator, and publisher cannot be set to empty as they are required.",
      responses = {
          @ApiResponse(responseCode = "200", description = "Digital object updated successfully"),
          @ApiResponse(responseCode = "400", description = "Invalid patch data or would violate constraints",
              content = @Content),
          @ApiResponse(responseCode = "404", description = "Digital object not found",
              content = @Content)
      }
  )
  public DigitalObjectCompactDTO patchDigitalObject(
      @PathVariable String projectAbbr,
      @PathVariable String id,
      @RequestBody DigitalObjectUpdateDto patch
  ) {
    projectService.verifyProjectAbbrMatchesObjectId(projectAbbr, id);
    return digitalObjectService.updateDigitalObject(id, patch);
  }

  @Hidden
  @PatchMapping(
      value = "/{id}",
      consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE
  )
  public String patchDigitalObjectFromForm(
      @PathVariable String projectAbbr,
      @PathVariable String id,
      @ModelAttribute DigitalObjectUpdateDto patch
  ) {
    projectService.verifyProjectAbbrMatchesObjectId(projectAbbr, id);

    // Parse comma-separated tags from form into the tags Set
    if (patch.getTagsCommaSeparated() != null) {
      Set<String> parsedTags = Arrays.stream(patch.getTagsCommaSeparated().split(","))
          .map(String::trim)
          .filter(s -> !s.isEmpty())
          .collect(Collectors.toSet());
      patch.setTags(parsedTags);
    }

    digitalObjectService.updateDigitalObject(id, patch);
    return "redirect:/api/curation/v1/projects/" + projectAbbr + "/objects/" + id;
  }

}
