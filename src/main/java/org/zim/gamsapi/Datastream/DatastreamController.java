package org.zim.gamsapi.Datastream;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.zim.gamsapi.Datastream.interfaces.IDatastreamDetailsView;
import org.zim.gamsapi.Datastream.interfaces.IDatastreamService;
import org.zim.gamsapi.DigitalObject.DigitalObject;
import org.zim.gamsapi.DigitalObject.interfaces.IDigitalObjectService;
import org.zim.gamsapi.Project.Project;
import org.zim.gamsapi.Project.interfaces.IProjectService;
import org.zim.gamsapi.System.utils.ControllerUtils;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@RequestMapping({"/api/v1/projects/{projectAbbr}/objects/{id}/datastreams/{dsid}", "/api/v1/projects/{projectAbbr}/objects/{id}/datastreams/{dsid}/"})
@Controller
public class DatastreamController {

  private final IDatastreamService datastreamService;
  private final IProjectService projectService;
  private final IDigitalObjectService digitalObjectService;

  @GetMapping(produces = MimeTypeUtils.TEXT_HTML_VALUE)
  public String getDatastream(Datastream datastream, DigitalObject digitalObject, Model model, Project project) {
    IDatastreamDetailsView foundDatastream = datastreamService.findDatastreamDetailsById(DatastreamId.builder().dsid(datastream.getDsid()).digitalObject(digitalObject.getId()).build());
    model.addAttribute("datastream", foundDatastream);
    model.addAttribute(project);
    return "Datastream/show";
  }

  @GetMapping(produces = MimeTypeUtils.APPLICATION_JSON_VALUE)
  @ResponseBody
  public IDatastreamDetailsView getDatastreamJson(Datastream datastream, DigitalObject digitalObject, Model model, Project project) {
    datastream.setDigitalObject(digitalObject);
    IDatastreamDetailsView foundDatastream = datastreamService.findDatastreamDetailsById(
        DatastreamId.builder().digitalObject(digitalObject.getId()).dsid(datastream.getDsid()).build());
    model.addAttribute(foundDatastream);
    model.addAttribute(project);
    return foundDatastream;
  }

  @PutMapping
  public String createDatastream(
          DigitalObject digitalObject,
          Datastream datastream,
          @RequestParam MultipartFile file,
          Model model,
          Project project,
          @RequestHeader Map<String, String> requestHeader
  ) throws IOException {

    log.debug("Got datastream-entity: {}. Applying file {} from request-params", datastream, file);

    // TODO remove outdated method? datastream can only be created via ingest`?

    DigitalObject foundObject = digitalObjectService.findById(digitalObject.getId());

    datastream.setData(file.getBytes());
    datastream.setMimeType(file.getContentType());
    datastream.setDigitalObject(foundObject);
    Datastream savedDatastream = datastreamService.save(datastream);

    model.addAttribute("datastream", savedDatastream);
    model.addAttribute(foundObject);
    String resolvedOrigin = ControllerUtils.resolveProxiedOrigin(requestHeader);
    return "redirect:" + resolvedOrigin + "api/v1/projects/" + project.getProjectAbbr() + "/objects/" + digitalObject.getId();
  }

  @DeleteMapping
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
   * @param datastream incoming datastream dto
   * @return binary-data of the datastream
   */
  @GetMapping( path = {"/content", "/content/"})
  @ResponseBody
  public ResponseEntity<InputStreamResource> getDatastreamContent(Datastream datastream) {
    // TODO add test
    Datastream foundDatastream = datastreamService.findById(datastream.deriveDatastreamId());
    InputStream in = new ByteArrayInputStream(foundDatastream.getData());
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(foundDatastream.getMimeType()))
        .body(new InputStreamResource(in));

  }
}
