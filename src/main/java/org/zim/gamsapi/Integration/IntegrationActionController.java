package org.zim.gamsapi.Integration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

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
  public List<IntegrationActionReport> indexProjectObject(@PathVariable String projectAbbr, @PathVariable String id){
    log.trace("*** Trying now to default index object {} for project {}", id, projectAbbr);
    ArrayList<IntegrationActionReport> indexingReports = new ArrayList<>();
    integrationServices.forEach(integrationService -> {
      indexingReports.addAll(integrationService.indexObject(projectAbbr, id));
    });
    return indexingReports;
  }

  @DeleteMapping("/{id}")
  public List<IntegrationActionReport> deleteProjectObject(@PathVariable String projectAbbr, @PathVariable String id){
    log.trace("*** Trying now to default delete object {} for project {}", id, projectAbbr);
    ArrayList<IntegrationActionReport> indexingReports = new ArrayList<>();
    integrationServices.forEach(integrationService -> {
      indexingReports.add(integrationService.deleteIndexedObject(projectAbbr, id));
    });
    return indexingReports;
  }

  @PostMapping
  public List<IntegrationActionReport> indexProjectObjects(@PathVariable String projectAbbr){
    log.trace("*** Trying now to default index all objects for project {}", projectAbbr);
    ArrayList<IntegrationActionReport> indexingReports = new ArrayList<>();
    integrationServices.forEach(integrationService -> {
      indexingReports.addAll(integrationService.indexObjects(projectAbbr));
    });
    return indexingReports;
  }

  @DeleteMapping
  public List<IntegrationActionReport> deleteProjectIndices(@PathVariable String projectAbbr){
    log.trace("*** Trying now to default delete all indices of objects for project {}", projectAbbr);
    ArrayList<IntegrationActionReport> indexingReports = new ArrayList<>();
    integrationServices.forEach(integrationService -> {
      indexingReports.add(integrationService.deleteIndexedObjects(projectAbbr));
    });
    return indexingReports;
  }

}
