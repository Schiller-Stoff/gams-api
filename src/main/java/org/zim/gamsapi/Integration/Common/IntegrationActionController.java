package org.zim.gamsapi.Integration.Common;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.zim.gamsapi.Integration.Common.interfaces.IIntegrationService;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping(value = {"/api/v1/integration/projects/{projectAbbr}/objects/", "/api/v1/integration/projects/{projectAbbr}/objects"})
@Slf4j
@RequiredArgsConstructor
@RestController
public class IntegrationActionController {

  /**
   * All beans injected that implement the interface.
   */
  private final List<IIntegrationService> integrationServices;

  @PostMapping("/{id}")
  public void indexProjectObject(@PathVariable String projectAbbr, @PathVariable String id){
    log.trace("*** Trying now to default index object {} for project {}", id, projectAbbr);
    integrationServices.forEach(integrationService -> {
      integrationService.indexObject(projectAbbr, id);
    });

  }

  @DeleteMapping("/{id}")
  public void deleteProjectObject(@PathVariable String projectAbbr, @PathVariable String id){
    log.trace("*** Trying now to default delete object {} for project {}", id, projectAbbr);
    integrationServices.forEach(integrationService -> {
      integrationService.deleteIndexedObject(projectAbbr, id);
    });
  }

  @PostMapping
  public void indexProjectObjects(@PathVariable String projectAbbr){
    log.trace("*** Trying now to default index all objects for project {}", projectAbbr);
    integrationServices.forEach(integrationService -> {
      integrationService.indexObjects(projectAbbr);
    });
  }

  @DeleteMapping
  public void deleteProjectIndices(@PathVariable String projectAbbr){
    log.trace("*** Trying now to default delete all indices of objects for project {}", projectAbbr);
    integrationServices.forEach(integrationService -> {
      integrationService.deleteIndexedObjects(projectAbbr);
    });
  }

}
