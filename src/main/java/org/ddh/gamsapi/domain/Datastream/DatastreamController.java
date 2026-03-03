package org.ddh.gamsapi.domain.Datastream;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ddh.gamsapi.domain.Datastream.utils.dto.DatastreamCreateDto;
import org.ddh.gamsapi.domain.Datastream.utils.dto.DatastreamUpdateDto;
import org.ddh.gamsapi.domain.Datastream.utils.interfaces.IDatastreamContentService;
import org.ddh.gamsapi.domain.Datastream.utils.interfaces.IDatastreamDetailsView;
import org.ddh.gamsapi.domain.Datastream.utils.interfaces.IDatastreamService;
import org.ddh.gamsapi.domain.DigitalObject.DigitalObject;
import org.ddh.gamsapi.domain.Project.Project;
import org.ddh.gamsapi.domain.Project.exceptions.ProjectNotFoundException;
import org.ddh.gamsapi.domain.Project.interfaces.IProjectService;
import org.ddh.gamsapi.infrastructure.System.config.OpenAPIConfig;
import org.ddh.gamsapi.infrastructure.System.dto.PagedResponse;
import org.ddh.gamsapi.infrastructure.System.security.DatastreamAuthorizationService;
import org.ddh.gamsapi.infrastructure.System.security.exceptions.UserNotAuthorizedException;
import org.ddh.gamsapi.infrastructure.System.utils.ControllerUtils;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@RequestMapping({"/api/v1/projects/{projectAbbr}/objects/{id}"})
@Controller
@Tag(name = OpenAPIConfig.DATASTREAMS_TAG, description = OpenAPIConfig.DATASTREAMS_TAG_DESCRIPTION)
public class DatastreamController {

  private final IDatastreamService datastreamService;
  private final IDatastreamContentService datastreamContentService;
  private final IProjectService projectService;
  private final DatastreamAuthorizationService datastreamAuthorizationService;


  @GetMapping(path = {"/datastream/content"})
  @ResponseBody
  @Operation(
      summary = "Get the content of the digital object's main datastream.",
      description = "Retrieves the binary content of the main datastream of defined digital object. Allows to return a different datastream content via specifying datastream tags.",
      responses = {
          @ApiResponse(responseCode = "200", description = "Datastream content",
              content = @Content(mediaType = MimeTypeUtils.APPLICATION_OCTET_STREAM_VALUE)),
          @ApiResponse(responseCode = "404", description = "Datastream not found", content = @Content),
          @ApiResponse(responseCode = "409", description = "Datastream ambiguous match: Defined tag variable must match exactly one datastream of the digital object.", content = @Content),
          @ApiResponse(responseCode = "500", description = "Main datastream is not defined.", content = @Content)
      }
  )
  @Parameter(name = "id", description = "ID of the digital object", required = true)
  @Parameter(name = "tag", description = "Tags of the datastream to retrieve. If not specified, the main datastream will be returned. (Defined tags must match exactly one datastream)")
  public ResponseEntity<InputStreamResource> getDatastreamContent(
      @PathVariable String projectAbbr,
      @PathVariable String id,
      @RequestParam(defaultValue = "", required = false, name = "tag") Set<String> tags,
      Authentication authentication
  ){

    if(!projectService.exists(projectAbbr)){
      throw new ProjectNotFoundException(
          "Datastream content not findable. Project does not exist: " + projectAbbr
      );
    }

    projectService.verifyProjectAbbrMatchesObjectId(projectAbbr, id);

    IDatastreamDetailsView foundDatastream;
    // use main datastream if no tags are provided
    if(tags.isEmpty()){
      foundDatastream =  datastreamService.findMainDatastreamByDigitalObjectId(id);
    } else {
      foundDatastream = datastreamService.findSingularDatastreamDetailsViewByObjectIdAndTags(id, tags);
    }

    // In DatastreamController — after resolving the datastream
    AuthorizationDecision decision = datastreamAuthorizationService.checkContentAccess(
        projectAbbr,
        foundDatastream.getContentRestrictions(),
        authentication
    );

    if (!decision.isGranted()) {
      throw new UserNotAuthorizedException(
          "Access denied to content of datastream " + foundDatastream.getDsid()
              + " on object " + id);
    }

    DatastreamId datastreamId = DatastreamId.builder()
        .dsid(foundDatastream.getDsid())
        .digitalObject(foundDatastream.getDigitalObject().getId())
        .build();

    InputStreamResource inputStreamResource = datastreamContentService.load(datastreamId);

    return ResponseEntity.ok()
        .contentLength(foundDatastream.getSize())
        .contentType(MediaType.parseMediaType(foundDatastream.getMimeType()))
        .body( inputStreamResource);

  }

  @GetMapping(path = {"/datastream"}, produces = {
      MimeTypeUtils.APPLICATION_JSON_VALUE,
      MimeTypeUtils.APPLICATION_XML_VALUE
  })
  @ResponseBody
  @Operation(
      summary = "Get datastream details",
      description = "Retrieves the details of a digital object's main datastream or a specific datastream by tags. If no tags are provided, the main datastream will be returned.",
      responses = {
          @ApiResponse(responseCode = "200", description = "Datastream details",
              content = @Content(mediaType = MimeTypeUtils.APPLICATION_JSON_VALUE)),
          @ApiResponse(responseCode = "404", description = "Datastream not found", content = @Content),
          @ApiResponse(responseCode = "409", description = "Datastream ambiguous match: Defined tag variable must match exactly one datastream of the digital object.", content = @Content),
          @ApiResponse(responseCode = "500", description = "Main datastream is not defined.", content = @Content)
      }

  )
  @Parameter(name = "id", description = "ID of the digital object", required = true)
  public IDatastreamDetailsView retrieveSingularDatastream(
      @PathVariable String projectAbbr,
      @PathVariable String id,
      @RequestParam(defaultValue = "", required = false, name = "tag") Set<String> tags
  ){

    if(!projectService.exists(projectAbbr)){
      throw new ProjectNotFoundException(
          "Datastream details not findable. Project does not exist: " + projectAbbr
      );
    }

    projectService.verifyProjectAbbrMatchesObjectId(projectAbbr, id);

    IDatastreamDetailsView foundDatastream;
    // use main datastream if no tags are provided
    if(tags.isEmpty()){
      foundDatastream =  datastreamService.findMainDatastreamByDigitalObjectId(id);
    } else {
      foundDatastream = datastreamService.findSingularDatastreamDetailsViewByObjectIdAndTags(id, tags);
    }
    return foundDatastream;
  }

  @GetMapping(path = {"/datastreams" }, produces = {
      MimeTypeUtils.APPLICATION_JSON_VALUE,
      MimeTypeUtils.APPLICATION_XML_VALUE
  })
  @ResponseBody
  @Operation(
      summary = "Get all datastreams details",
      description = "Retrieves all datastreams details of a digital object. Allows to filter by tags and paginate results.",
      responses = {
          @ApiResponse(responseCode = "200", description = "List of datastreams",
              content = @Content(mediaType = MimeTypeUtils.APPLICATION_JSON_VALUE)),
          @ApiResponse(responseCode = "404", description = "Digital object / project not found", content = @Content)
      }

  )
  @Parameter(name = "projectAbbr", description = "Project abbreviation of the GAMS project", required = true)
  @Parameter(name = "id", description = "ID of the digital object", required = true)
  public PagedResponse<IDatastreamDetailsView> findAllDatastreams(
      @PathVariable String projectAbbr,
      @PathVariable String id,
      @RequestParam(defaultValue = "", required = false, name = "tag") Set<String> tags,
      // for pagination
      @RequestParam(defaultValue = "0") int pageIndex,
      @RequestParam(defaultValue = "100") int pageSize,
      @RequestParam(defaultValue = "dsid") String sortBy
  ) {

    if(!projectService.exists(projectAbbr)){
      throw new ProjectNotFoundException(
          "Datastream list not findable. Project does not exist: " + projectAbbr
      );
    }

    projectService.verifyProjectAbbrMatchesObjectId(projectAbbr, id);

    // limit pageSize to max 100
    if (pageSize >= 100) {
      pageSize = 100;
    }

    // return just paging information if no tags are provided
    if(tags.isEmpty()){
      return datastreamService.findAll(
          id,
          PageRequest.of(pageIndex, pageSize, Sort.by(sortBy))
      );
    }

    return datastreamService.findAll(
        id,
        tags,
        PageRequest.of(pageIndex, pageSize, Sort.by(sortBy))
    );

  }


  /**
   * Editable MIME types for the web code editor.
   * These are text-based formats a user may want to edit inline.
   */
  private static final Set<String> EDITABLE_MIME_TYPES = Set.of(
      "application/json",
      "application/xml",
      "text/xml",
      "text/html",
      "text/plain",
      "text/csv",
      "text/css",
      "application/javascript",
      "text/javascript",
      "application/xhtml+xml",
      "application/rdf+xml",
      "application/ld+json",
      "text/turtle",
      "text/markdown"
  );

  @GetMapping(
      path = {"/datastreams/{dsid}"},
      produces = MimeTypeUtils.TEXT_HTML_VALUE
  )
  public String getDatastream(
      Datastream datastream,
      DigitalObject digitalObject,
      Model model,
      Project project,
      Authentication authentication
  ) {
    IDatastreamDetailsView foundDatastream = datastreamService.findDatastreamDetailsById(
        DatastreamId.builder()
            .dsid(datastream.getDsid())
            .digitalObject(digitalObject.getId())
            .build()
    );

    model.addAttribute("datastream", foundDatastream);
    model.addAttribute("digitalObject", digitalObject);
    model.addAttribute(project);

    // Authentication state for conditional rendering of edit forms
    boolean canEdit = authentication != null
        && authentication.isAuthenticated()
        && !(authentication instanceof AnonymousAuthenticationToken);
    model.addAttribute("isAuthenticated", canEdit);

    // Pre-sorted CSV strings for tags, lang and contentRestrictions form fields
    model.addAttribute("sortedTagsCsv",
        foundDatastream.getTags() != null
            ? foundDatastream.getTags().stream().sorted().collect(Collectors.joining(", "))
            : "");
    model.addAttribute("sortedLangCsv",
        foundDatastream.getLang() != null
            ? foundDatastream.getLang().stream().sorted().collect(Collectors.joining(", "))
            : "");
    model.addAttribute("sortedContentRestrictionsCsv",
        foundDatastream.getContentRestrictions() != null
            ? foundDatastream.getContentRestrictions().stream().sorted().collect(Collectors.joining(", "))
            : "");

    // Determine if content is editable in the web code editor
    boolean isEditable = canEdit
        && foundDatastream.getMimeType() != null
        && EDITABLE_MIME_TYPES.contains(foundDatastream.getMimeType().toLowerCase());
    model.addAttribute("isEditable", isEditable);

    return "Datastream/show";
  }

  @GetMapping(
      path = {"/datastreams/{dsid}" },
      produces = {
          MimeTypeUtils.APPLICATION_JSON_VALUE,
          MimeTypeUtils.APPLICATION_XML_VALUE
      }
  )
  @ResponseBody
  @Operation(
      summary = "Get datastream details as JSON",
      description = "Retrieves the details of a specific datastream by its ID in JSON format.",
      responses = {
          @ApiResponse(responseCode = "200", description = "Datastream details in JSON format",
              content = @Content(mediaType = MimeTypeUtils.APPLICATION_JSON_VALUE)),
          @ApiResponse(responseCode = "404", description = "Datastream not found", content = @Content)
      }
  )
  @Parameter(name = "projectAbbr", description = "Project abbreviation of the GAMS project", required = true)
  @Parameter(name = "id", description = "ID of the digital object", required = true)
  @Parameter(name = "dsid", description = "ID of the datastream", required = true)
  public IDatastreamDetailsView getDatastreamJson(
      @PathVariable String projectAbbr,
      @PathVariable String id,
      @PathVariable String dsid
  ) {

    if(!projectService.exists(projectAbbr)){
      throw new ProjectNotFoundException(
          "Cannot retrieve datastream details. Project does not exist: " + projectAbbr
      );
    }

    projectService.verifyProjectAbbrMatchesObjectId(projectAbbr, id);

    DigitalObject digitalObject = new DigitalObject();
    digitalObject.setId(id);
    Datastream datastream = new DatastreamBuilder()
        .dsid(dsid)
        .digitalObject(id)
        .build();
    datastream.setDigitalObject(digitalObject);

    return datastreamService.findDatastreamDetailsById(
        DatastreamId.builder()
            .digitalObject(digitalObject.getId())
            .dsid(datastream.getDsid())
            .build()
    );
  }


  /**
   * Dynamically (according to mimetype) returns stored datastream content
   * <a href="https://www.baeldung.com/spring-controller-return-image-file">Return image via spring baeldung</a>
   * @param id digital-object-id
   * @param dsid datastream-id
   * @return binary-data of the datastream
   */
  @GetMapping( path = {"/datastreams/{dsid}/content" })
  @ResponseBody
  @Operation(
      summary = "Get datastream content",
      description = "Retrieves the binary content of a specific datastream by its ID. The content type is determined by the datastream's MIME type."
  )
  @Parameter(name = "id", description = "ID of the digital object", required = true)
  @Parameter(name = "dsid", description = "ID of the datastream", required = true)
  public ResponseEntity<InputStreamResource> getDatastreamContent(
      @PathVariable String projectAbbr,
      @PathVariable String id,
      @PathVariable String dsid,
      Authentication authentication
  ) {
    if (!projectService.exists(projectAbbr)) {
      throw new ProjectNotFoundException(
          "Cannot retrieve datastream content. Project does not exist: " + projectAbbr);
    }

    projectService.verifyProjectAbbrMatchesObjectId(projectAbbr, id);

    Datastream datastream = datastreamService.findById(
        DatastreamId.builder()
            .digitalObject(id)
            .dsid(dsid)
            .build()
    );

    // Content authorization — uses already-loaded datastream
    AuthorizationDecision decision = datastreamAuthorizationService.checkContentAccess(
        projectAbbr,
        datastream.getContentRestrictions(),
        authentication
    );
    if (!decision.isGranted()) {
      throw new UserNotAuthorizedException(
          "Access denied to content of datastream " + dsid + " on object " + id);
    }

    InputStreamResource inputStreamResource = datastreamContentService.load(
        datastream.deriveDatastreamId());

    return ResponseEntity.ok()
        .contentLength(datastream.getSize())
        .contentType(MediaType.parseMediaType(datastream.getMimeType()))
        .body(inputStreamResource);
  }

  @Operation(
      summary = "Get all datastream dsids for a digital object",
      description = "Retrieves a paginated list of all datastream dsids for a specific digital object"
  )
  @GetMapping(value = "/datastreams/dsids", produces = {
      MimeTypeUtils.APPLICATION_JSON_VALUE,
      MimeTypeUtils.APPLICATION_XML_VALUE
  })
  @ResponseBody
  public PagedResponse<String> findAllIdsByDigitalObject(
      @PathVariable String id,
      @RequestParam(defaultValue = "0") int pageIndex,
      @RequestParam(defaultValue = "10000") int pageSize,
      @RequestParam(defaultValue = "dsid") String sortBy
  ) {
    // limit pageSize
    if (pageSize >= 10000) {
      pageSize = 10000;
    }

    return datastreamService.findAllIds(
        id,
        PageRequest.of(pageIndex, pageSize, Sort.by(sortBy))
    );
  }

  @PutMapping(
      path = "/datastreams/{dsid}",
      consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
      produces = MimeTypeUtils.APPLICATION_JSON_VALUE
  )
  @ResponseBody
  @Operation(
      summary = "Create a new datastream",
      description = "Creates a new datastream with the given DSID on the specified digital object. "
          + "The file content is uploaded as multipart. "
          + "Checksums (MD5, SHA-512) are computed server-side during file write. "
          + "Returns 409 Conflict if a datastream with this DSID already exists. "
          + "Requires authentication and project membership.",
      responses = {
          @ApiResponse(responseCode = "201", description = "Datastream created successfully",
              content = @Content(mediaType = MimeTypeUtils.APPLICATION_JSON_VALUE)),
          @ApiResponse(responseCode = "400", description = "Invalid input or file missing",
              content = @Content),
          @ApiResponse(responseCode = "404", description = "Project or digital object not found",
              content = @Content),
          @ApiResponse(responseCode = "409", description = "Datastream with this DSID already exists",
              content = @Content)
      }
  )
  @Parameter(name = "projectAbbr", description = "Project abbreviation", required = true)
  @Parameter(name = "id", description = "ID of the digital object", required = true)
  @Parameter(name = "dsid", description = "Datastream identifier (e.g. my_image.jpg)", required = true)
  public ResponseEntity<IDatastreamDetailsView> createDatastreamJson(
      @PathVariable String projectAbbr,
      @PathVariable String id,
      @PathVariable String dsid,
      @Valid DatastreamCreateDto dto,
      @RequestParam("file") MultipartFile file
  ) {
    if (!projectService.exists(projectAbbr)) {
      throw new ProjectNotFoundException(
          "Cannot create datastream. Project does not exist: " + projectAbbr
      );
    }
    projectService.verifyProjectAbbrMatchesObjectId(projectAbbr, id);

    Datastream created = datastreamService.createFromUpload(id, dsid, dto, file);

    IDatastreamDetailsView view = datastreamService
        .findDatastreamDetailsById(created.deriveDatastreamId());

    return ResponseEntity
        .status(HttpStatus.CREATED)
        .body(view);
  }

  @Hidden
  @PostMapping(
      path = "/datastreams",
      consumes = MediaType.MULTIPART_FORM_DATA_VALUE
  )
  public String createDatastreamFromForm(
      @PathVariable String projectAbbr,
      @PathVariable String id,
      @RequestParam("dsid") String dsid,
      @Valid DatastreamCreateDto dto,
      @RequestParam("file") MultipartFile file,
      @RequestHeader Map<String, String> requestHeader
  ) {
    datastreamService.createFromUpload(id, dsid, dto, file);
    String origin = ControllerUtils.resolveProxiedOrigin(requestHeader);
    return "redirect:" + origin + "api/v1/projects/" + projectAbbr + "/objects/" + id;
  }

  @DeleteMapping(value = "/datastreams/{dsid}")
  @ResponseBody
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
      summary = "Delete a datastream by its DSID",
      description = "Permanently deletes a datastream and its file content from the specified digital object. "
          + "This operation is irreversible. Requires authentication and project membership.",
      responses = {
          @ApiResponse(responseCode = "204", description = "Datastream deleted successfully"),
          @ApiResponse(responseCode = "404", description = "Project, digital object, or datastream not found",
              content = @Content),
          @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content)
      }
  )
  @Parameter(name = "projectAbbr", description = "Project abbreviation", required = true)
  @Parameter(name = "id", description = "ID of the digital object", required = true)
  @Parameter(name = "dsid", description = "ID of the datastream to delete", required = true)
  public void deleteDatastreamJson(
      @PathVariable String projectAbbr,
      @PathVariable String id,
      @PathVariable String dsid
  ) {
    if (!projectService.exists(projectAbbr)) {
      throw new ProjectNotFoundException(
          "Cannot delete datastream. Project does not exist: " + projectAbbr
      );
    }

    projectService.verifyProjectAbbrMatchesObjectId(projectAbbr, id);

    Datastream datastream = datastreamService.findById(
        DatastreamId.builder()
            .digitalObject(id)
            .dsid(dsid)
            .build()
    );

    datastreamService.delete(datastream);
  }

  @Hidden
  @DeleteMapping(value = "/datastreams/{dsid}", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
  public String deleteDatastreamHtml(
      @PathVariable String projectAbbr,
      @PathVariable String id,
      @PathVariable String dsid,
      @RequestHeader Map<String, String> requestHeader
  ) {
    if (!projectService.exists(projectAbbr)) {
      throw new ProjectNotFoundException(
          "Cannot delete datastream. Project does not exist: " + projectAbbr
      );
    }

    projectService.verifyProjectAbbrMatchesObjectId(projectAbbr, id);

    Datastream datastream = datastreamService.findById(
        DatastreamId.builder()
            .digitalObject(id)
            .dsid(dsid)
            .build()
    );

    datastreamService.delete(datastream);

    String origin = ControllerUtils.resolveProxiedOrigin(requestHeader);
    return "redirect:" + origin + "api/v1/projects/" + projectAbbr + "/objects/" + id;
  }


  @PatchMapping(
      path = "/datastreams/{dsid}",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MimeTypeUtils.APPLICATION_JSON_VALUE
  )
  @ResponseBody
  @Operation(
      summary = "Update datastream metadata",
      description = "Partially updates the metadata of an existing datastream. "
          + "Only fields present in the request body are updated; omitted fields remain unchanged. "
          + "The dsid and parent digital object cannot be changed. "
          + "Fields like title, rights, and creator cannot be set to empty as they are required.",
      responses = {
          @ApiResponse(responseCode = "200", description = "Datastream metadata updated successfully"),
          @ApiResponse(responseCode = "400", description = "Invalid patch data or would violate constraints",
              content = @Content),
          @ApiResponse(responseCode = "404", description = "Datastream or digital object not found",
              content = @Content)
      }
  )
  @Parameter(name = "projectAbbr", description = "Project abbreviation", required = true)
  @Parameter(name = "id", description = "ID of the digital object", required = true)
  @Parameter(name = "dsid", description = "Datastream identifier", required = true)
  public IDatastreamDetailsView patchDatastream(
      @PathVariable String projectAbbr,
      @PathVariable String id,
      @PathVariable String dsid,
      @RequestBody DatastreamUpdateDto patch
  ) {
    projectService.verifyProjectAbbrMatchesObjectId(projectAbbr, id);
    return datastreamService.updateDatastream(id, dsid, patch);
  }

  @Hidden
  @PatchMapping(
      path = "/datastreams/{dsid}",
      consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE
  )
  public String patchDatastreamFromForm(
      @PathVariable String projectAbbr,
      @PathVariable String id,
      @PathVariable String dsid,
      @ModelAttribute DatastreamUpdateDto patch
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

    // Parse comma-separated lang from form into the lang Set
    if (patch.getLangCommaSeparated() != null) {
      Set<String> parsedLang = Arrays.stream(patch.getLangCommaSeparated().split(","))
          .map(String::trim)
          .filter(s -> !s.isEmpty())
          .collect(Collectors.toSet());
      patch.setLang(parsedLang);
    }

    // Parse comma-separated content restrictions from form
    if (patch.getContentRestrictionsCommaSeparated() != null) {
      Set<String> parsedRestrictions = Arrays.stream(
              patch.getContentRestrictionsCommaSeparated().split(","))
          .map(String::trim)
          .filter(s -> !s.isEmpty())
          .map(String::toUpperCase) // normalize to uppercase
          .collect(Collectors.toSet());
      patch.setContentRestrictions(parsedRestrictions);
    }

    datastreamService.updateDatastream(id, dsid, patch);
    return "redirect:/api/v1/projects/" + projectAbbr + "/objects/" + id + "/datastreams/" + dsid;
  }


  // ==================================================================================
  // PUT CONTENT (file replacement)
  // ==================================================================================

  @PostMapping(
      path = "/datastreams/{dsid}/content",
      consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
      produces = MimeTypeUtils.APPLICATION_JSON_VALUE
  )
  @ResponseBody
  @Operation(
      summary = "Update datastream content",
      description = "Replaces the binary content of an existing datastream with a new file upload. "
          + "Checksums (MD5, SHA-512) are recomputed server-side during file write. "
          + "File size and MIME type are updated automatically. "
          + "Metadata (title, description, etc.) remains unchanged. "
          + "Requires authentication and project membership.",
      responses = {
          @ApiResponse(responseCode = "200", description = "Datastream content updated successfully",
              content = @Content(mediaType = MimeTypeUtils.APPLICATION_JSON_VALUE)),
          @ApiResponse(responseCode = "400", description = "File is missing or empty",
              content = @Content),
          @ApiResponse(responseCode = "404", description = "Datastream or digital object not found",
              content = @Content)
      }
  )
  @Parameter(name = "projectAbbr", description = "Project abbreviation", required = true)
  @Parameter(name = "id", description = "ID of the digital object", required = true)
  @Parameter(name = "dsid", description = "Datastream identifier", required = true)
  public IDatastreamDetailsView updateDatastreamContent(
      @PathVariable String projectAbbr,
      @PathVariable String id,
      @PathVariable String dsid,
      @RequestParam("file") MultipartFile file
  ) {
    projectService.verifyProjectAbbrMatchesObjectId(projectAbbr, id);
    return datastreamService.updateDatastreamContent(id, dsid, file);
  }


}
