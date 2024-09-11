package org.zim.gamsapi.Datastream;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.zim.gamsapi.Datastream.interfaces.IDatastreamDetailsView;
import org.zim.gamsapi.Datastream.interfaces.IDatastreamService;
import org.zim.gamsapi.Datastream.interfaces.IDatastreamContentService;
import org.zim.gamsapi.DigitalObject.DigitalObject;
import org.zim.gamsapi.DigitalObject.interfaces.IDigitalObjectService;
import org.zim.gamsapi.MetadataBaseEntity;
import org.zim.gamsapi.Project.Project;
import org.zim.gamsapi.Project.interfaces.IProjectService;
import org.zim.gamsapi.System.utils.ControllerUtils;

import java.io.IOException;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@RequestMapping({"/api/v1/projects/{projectAbbr}/objects/{id}/datastreams/{dsid}", "/api/v1/projects/{projectAbbr}/objects/{id}/datastreams/{dsid}/"})
@Controller
public class DatastreamController {

  private final IDatastreamService datastreamService;
  private final IProjectService projectService;
  private final IDigitalObjectService digitalObjectService;
  private final IDatastreamContentService datastreamContentService;

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

  @PutMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public String createDatastream(
          DigitalObject digitalObject,
          Datastream datastream,
          @RequestParam MetadataBaseEntity metadataBaseEntity,
          @RequestParam MultipartFile file,
          Model model,
          Project project,
          @RequestHeader Map<String, String> requestHeader
  ) {

    // TODO: is this method outdated? - datastreams need baseMetadata assigned.
    // TODO: could move this to service method

    log.debug("Got datastream-entity: {}. Applying file {} from request-params", datastream, file);

    DigitalObject foundObject = digitalObjectService.findById(digitalObject.getId());


    datastream.setMimeType(file.getContentType());
    datastream.setDigitalObject(foundObject);

    // TODO test if setting of baseMetadata works as expected!
    datastream.setBaseMetadata(metadataBaseEntity);
    Datastream savedDatastream = datastreamService.save(datastream, file);

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
   * @param id digital-object-id
   * @param dsid datastream-id
   * @return binary-data of the datastream
   * // TODO write test
   */
  @GetMapping( path = {"/content", "/content/"})
  @ResponseBody
  public ResponseEntity<Resource> getDatastreamContent(@PathVariable String id, @PathVariable String dsid) {
    Datastream datastream = new DatastreamBuilder().dsid(dsid).digitalObject(id).build();
    datastream = datastreamService.findById(datastream.deriveDatastreamId());
    FileSystemResource fileSystemResource = datastreamContentService.loadFile(datastream.deriveDatastreamId().toString());
    // TODO check if file streaming works this way! (maybe not using FileSystemResource but InputStreamResource etc.)?
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(datastream.getMimeType()))
        .body(fileSystemResource);

  }
}
