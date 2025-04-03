package org.zim.gamsapi.Datastream;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.*;
import org.zim.gamsapi.Datastream.interfaces.IDatastreamDetailsView;
import org.zim.gamsapi.Datastream.interfaces.IDatastreamService;
import org.zim.gamsapi.Datastream.interfaces.IDatastreamContentService;
import org.zim.gamsapi.DigitalObject.DigitalObject;
import org.zim.gamsapi.DigitalObject.DigitalObjectBuilder;
import org.zim.gamsapi.Project.Project;
import org.zim.gamsapi.Project.ProjectBuilder;
import org.zim.gamsapi.System.utils.ControllerUtils;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@RequiredArgsConstructor
@RequestMapping({"/api/v1/projects/{projectAbbr}/objects/{id}"})
@Controller
public class DatastreamController {

  private final IDatastreamService datastreamService;
  private final IDatastreamContentService datastreamContentService;


  @GetMapping(path = {"/datastream/content", "/datastream/content/"})
  @ResponseBody
  @Operation(summary = "Get datastream content")
  @Parameter(name = "id", description = "ID of the digital object", required = true)
  public ResponseEntity<InputStreamResource> getDatastreamContent(
      @PathVariable String id,
      @RequestParam(defaultValue = "", required = false, name = "tag") Set<String> tags
  ){

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

  @GetMapping(path = {"/datastream", "/datastream/"})
  @ResponseBody
  @Operation(summary = "Get datastream details")
  @Parameter(name = "id", description = "ID of the digital object", required = true)
  public IDatastreamDetailsView retrieveSingularDatastream(
      @PathVariable String id,
      @RequestParam(defaultValue = "", required = false, name = "tag") Set<String> tags
  ){
    IDatastreamDetailsView foundDatastream;
    // use main datastream if no tags are provided
    if(tags.isEmpty()){
      foundDatastream =  datastreamService.findMainDatastreamByDigitalObjectId(id);
    } else {
      foundDatastream = datastreamService.findSingularDatastreamDetailsViewByObjectIdAndTags(id, tags);
    }
    return foundDatastream;
  }

  @GetMapping(path = {"/datastreams", "/datastreams/"})
  @ResponseBody
  @Operation(summary = "Get all datastreams")
  @Parameter(name = "id", description = "ID of the digital object", required = true)
  public Page<IDatastreamDetailsView> findAllDatastreams(
      @PathVariable String id,
      // for pagination
      @RequestParam(defaultValue = "0") int pageIndex,
      @RequestParam(defaultValue = "100") int pageSize,
      @RequestParam(defaultValue = "dsid") String sortBy
  ) {

    // limit pageSize to max 100
    if (pageSize >= 100) {
      pageSize = 100;
    }

    return datastreamService.findAll(
        id,
        PageRequest.of(pageIndex, pageSize, Sort.by(sortBy))
    );
  }


  @GetMapping(
      path = {"/datastreams/{dsid}", "/datastreams/{dsid}/"},
      produces = MimeTypeUtils.TEXT_HTML_VALUE
  )
  public String getDatastream(Datastream datastream, DigitalObject digitalObject, Model model, Project project) {
    IDatastreamDetailsView foundDatastream = datastreamService.findDatastreamDetailsById(DatastreamId.builder().dsid(datastream.getDsid()).digitalObject(digitalObject.getId()).build());
    model.addAttribute("datastream", foundDatastream);
    model.addAttribute(project);
    return "Datastream/show";
  }

  @GetMapping(
      path = {"/datastreams/{dsid}", "/datastreams/{dsid}/"},
      produces = MimeTypeUtils.APPLICATION_JSON_VALUE
  )
  @ResponseBody
  @Operation(summary = "Get datastream details as JSON")
  public IDatastreamDetailsView getDatastreamJson(@PathVariable String dsid, @PathVariable String id, Model model, @PathVariable String projectAbbr) {
    DigitalObject digitalObject = new DigitalObject();
    digitalObject.setId(id);
    Datastream datastream = new DatastreamBuilder()
        .dsid(dsid)
        .digitalObject(id)
        .build();
    datastream.setDigitalObject(digitalObject);

    Project project = ProjectBuilder.builder()
        .projectAbbr(projectAbbr)
        .description("")
        .build();

    IDatastreamDetailsView foundDatastream = datastreamService.findDatastreamDetailsById(
        DatastreamId.builder().digitalObject(digitalObject.getId()).dsid(datastream.getDsid()).build());
    model.addAttribute(foundDatastream);
    model.addAttribute(project);
    return foundDatastream;
  }

  @Hidden
  @DeleteMapping(path = {"/datastreams/{dsid}", "/datastreams/{dsid}/"})
  public String deleteDatastream(
          @PathVariable String id,
          @PathVariable String dsid,
          Project project,
          @RequestHeader Map<String, String> requestHeader
  ) {

    Datastream datastream = new DatastreamBuilder()
        .dsid(dsid)
        .digitalObject(id)
        .build();

    datastreamService.delete(datastream);
    log.info("Successfully deleted datastream: {}", datastream);
    String resolvedOrigin = ControllerUtils.resolveProxiedOrigin(requestHeader);
    return "redirect:" + resolvedOrigin + "api/v1/projects/" + project.getProjectAbbr() + "/objects/" + datastream.getDigitalObject().getId();
  }



  /**
   * Dynamically (according to mimetype) returns stored datastream content
   * https://www.baeldung.com/spring-controller-return-image-file
   * @param id digital-object-id
   * @param dsid datastream-id
   * @return binary-data of the datastream
   */
  @GetMapping( path = {"/datastreams/{dsid}/content", "/datastreams/{dsid}/content/"})
  @ResponseBody
  @Operation(summary = "Get datastream content")
  @Parameter(name = "id", description = "ID of the digital object", required = true)
  @Parameter(name = "dsid", description = "ID of the datastream", required = true)
  public ResponseEntity<InputStreamResource> getDatastreamContent(
      @PathVariable String id,
      @PathVariable String dsid
  ){
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
}
