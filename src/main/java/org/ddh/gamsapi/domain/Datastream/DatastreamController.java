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
import org.ddh.gamsapi.domain.Datastream.utils.interfaces.IDatastreamContentService;
import org.ddh.gamsapi.domain.Datastream.utils.interfaces.IDatastreamDetailsView;
import org.ddh.gamsapi.domain.Datastream.utils.interfaces.IDatastreamService;
import org.ddh.gamsapi.domain.DigitalObject.DigitalObject;
import org.ddh.gamsapi.domain.Project.Project;
import org.ddh.gamsapi.domain.Project.exceptions.ProjectNotFoundException;
import org.ddh.gamsapi.domain.Project.interfaces.IProjectService;
import org.ddh.gamsapi.infrastructure.System.config.OpenAPIConfig;
import org.ddh.gamsapi.infrastructure.System.dto.PagedResponse;
import org.ddh.gamsapi.infrastructure.System.utils.ControllerUtils;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.Set;

@Slf4j
@RequiredArgsConstructor
@RequestMapping({"/api/v1/projects/{projectAbbr}/objects/{id}"})
@Controller
@Tag(name = OpenAPIConfig.DATASTREAMS_TAG, description = OpenAPIConfig.DATASTREAMS_TAG_DESCRIPTION)
public class DatastreamController {

  private final IDatastreamService datastreamService;
  private final IDatastreamContentService datastreamContentService;
  private final IProjectService projectService;


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
      @RequestParam(defaultValue = "", required = false, name = "tag") Set<String> tags
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


  @GetMapping(
      path = {"/datastreams/{dsid}" },
      produces = MimeTypeUtils.TEXT_HTML_VALUE
  )
  public String getDatastream(Datastream datastream, DigitalObject digitalObject, Model model, Project project) {
    IDatastreamDetailsView foundDatastream = datastreamService.findDatastreamDetailsById(DatastreamId.builder().dsid(datastream.getDsid()).digitalObject(digitalObject.getId()).build());
    model.addAttribute("datastream", foundDatastream);
    model.addAttribute(project);
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
      @PathVariable String dsid
  ){

    if(!projectService.exists(projectAbbr)){
      throw new ProjectNotFoundException(
          "Cannot retrieve datastream content. Project does not exist: " + projectAbbr
      );
    }

    projectService.verifyProjectAbbrMatchesObjectId(projectAbbr, id);

    Datastream datastream = new DatastreamBuilder()
        .dsid(dsid)
        .digitalObject(id)
        .build();

    datastream = datastreamService
        .findById(datastream.deriveDatastreamId());

    InputStreamResource inputStreamResource = datastreamContentService.load(datastream.deriveDatastreamId());

    return ResponseEntity.ok()
        .contentLength(datastream.getSize())
        .contentType(MediaType.parseMediaType(datastream.getMimeType()))
        .body( inputStreamResource);

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

  /**
   * REST API: Create a new datastream via file upload.
   * Checksums (MD5 + SHA-512) are computed server-side during file write.
   */
  @PostMapping(
      path = "/datastreams",
      consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
      produces = MimeTypeUtils.APPLICATION_JSON_VALUE
  )
  @ResponseBody
  @Operation(
      summary = "Create a new datastream",
      description = "Uploads a file as a new datastream for the specified digital object. "
          + "The DSID is derived from the uploaded filename. "
          + "Checksums (MD5, SHA-512) are computed server-side during file write. "
          + "Requires authentication.",
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
  public ResponseEntity<IDatastreamDetailsView> createDatastreamJson(
      @PathVariable String projectAbbr,
      @PathVariable String id,
      @Valid DatastreamCreateDto dto,
      @RequestParam("file") MultipartFile file
  ) {
    if (!projectService.exists(projectAbbr)) {
      throw new ProjectNotFoundException(
          "Cannot create datastream. Project does not exist: " + projectAbbr
      );
    }
    projectService.verifyProjectAbbrMatchesObjectId(projectAbbr, id);

    Datastream created = datastreamService.createFromUpload(id, dto, file);

    IDatastreamDetailsView view = datastreamService
        .findDatastreamDetailsById(created.deriveDatastreamId());

    return ResponseEntity
        .status(HttpStatus.CREATED)
        .body(view);
  }

  /**
   * Webclient: Create a new datastream via HTML form.
   */
  @Hidden
  @PostMapping(
      path = "/datastreams",
      consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
      produces = MimeTypeUtils.TEXT_HTML_VALUE
  )
  public String createDatastreamFromForm(
      @PathVariable String projectAbbr,
      @PathVariable String id,
      @Valid DatastreamCreateDto dto,
      @RequestParam("file") MultipartFile file,
      @RequestHeader Map<String, String> requestHeader
  ) {

    datastreamService.createFromUpload(id, dto, file);
    String origin = ControllerUtils.resolveProxiedOrigin(requestHeader);
    return "redirect:" + origin + "api/v1/projects/" + projectAbbr + "/objects/" + id;
  }


}
