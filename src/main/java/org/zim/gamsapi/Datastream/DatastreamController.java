package org.zim.gamsapi.Datastream;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
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
@RequestMapping({"/api/v1/management/projects/{projectAbbr}/objects/{pid}/datastreams/{dsid}", "/api/v1/management/projects/{projectAbbr}/objects/{pid}/datastreams/{dsid}/"})
@Controller
public class DatastreamController {

  private final IDatastreamService datastreamService;
  private final IProjectService projectService;
  private final IDigitalObjectService digitalObjectService;

  @GetMapping
  public String getDatastream(Datastream datastream, DigitalObject digitalObject, Model model, Project project) {
    Datastream foundDatastream = datastreamService.findByDsid(digitalObject.getPid(), datastream.getDsid());
    Project foundProject = projectService.getUserProjectByEntity(project);
    model.addAttribute(foundDatastream);
    model.addAttribute(foundProject);
    return "Datastream/show";
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

    //TODO refactor - might be encapsulated in business layer?

    DigitalObject foundObject = digitalObjectService.findByPid(digitalObject.getPid());

    datastream.setData(file.getBytes());
    datastream.setMimeType(file.getContentType());
    datastream.setDigitalObject(foundObject);
    Datastream savedDatastream = datastreamService.save(datastream);

    model.addAttribute("datastream", savedDatastream);
    model.addAttribute(foundObject);
    String resolvedOrigin = ControllerUtils.resolveProxiedOrigin(requestHeader);
    return "redirect:" + resolvedOrigin + "api/v1/management/projects/" + project.getProjectAbbr() + "/objects/" + digitalObject.getPid();
  }

  @DeleteMapping
  public String deleteDatastream(
          DigitalObject digitalObject,
          Datastream datastream,
          Project project,
          @RequestHeader Map<String, String> requestHeader
  ) {
    datastreamService.delete(digitalObject, datastream.getDsid());
    String resolvedOrigin = ControllerUtils.resolveProxiedOrigin(requestHeader);
    return "redirect:" + resolvedOrigin + "api/v1/management/projects/" + project.getProjectAbbr() + "/objects/" + digitalObject.getPid();
  }


  /**
   * Dynamically (according to mimetype) returns stored datastream content
   * https://www.baeldung.com/spring-controller-return-image-file
   * @param digitalObject incoming digital object dto
   * @param datastream incoming datastream dto
   * @return binary-data of the datastream
   */
  @GetMapping( path = {"/content", "/content/"})
  @ResponseBody
  public ResponseEntity<InputStreamResource> getDatastreamContent(DigitalObject digitalObject, Datastream datastream) {
    Datastream foundDatastream = datastreamService.findByDsid(digitalObject.getPid(), datastream.getDsid());
    InputStream in = new ByteArrayInputStream(foundDatastream.getData());
    return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(foundDatastream.getMimeType()))
            .body(new InputStreamResource(in));

  }

}
