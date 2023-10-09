package org.zim.gamsapi.Integration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping(value = {"/api/v1/integration/projects/{projectAbbr}/", "/api/v1/integration/projects/{projectAbbr}"})
@Slf4j
@RequiredArgsConstructor
@RestController
public class IntegrationActionController {

  /**
   * All beans injected that implement the interface.
   */
  private final List<IIntegrationService> integrationServices;

  @PostMapping("/objects/{id}")
  public List<IndexingReport> indexProjectObject(@PathVariable String projectAbbr, @PathVariable String id){
    log.trace("*** Trying now to default index object {} for project {}", id, projectAbbr);
    ArrayList<IndexingReport> indexingReports = new ArrayList<>();
    integrationServices.forEach(integrationService -> {
      indexingReports.addAll(integrationService.indexObject(projectAbbr, id));
    });
    return indexingReports;
  }

  @DeleteMapping("/objects/{id}")
  public List<IndexingReport> deleteProjectObject(@PathVariable String projectAbbr, @PathVariable String id){
    log.trace("*** Trying now to default delete object {} for project {}", id, projectAbbr);
    ArrayList<IndexingReport> indexingReports = new ArrayList<>();
    integrationServices.forEach(integrationService -> {
      indexingReports.add(integrationService.deleteIndexedObject(projectAbbr, id));
    });
    return indexingReports;
  }

  @PostMapping
  public List<IndexingReport> indexProjectObjects(@PathVariable String projectAbbr){
    log.trace("*** Trying now to default index all objects for project {}", projectAbbr);
    ArrayList<IndexingReport> indexingReports = new ArrayList<>();
    integrationServices.forEach(integrationService -> {
      indexingReports.addAll(integrationService.indexObjects(projectAbbr));
    });
    return indexingReports;
  }

  @DeleteMapping
  public List<IndexingReport> deleteProjectIndices(@PathVariable String projectAbbr){
    log.trace("*** Trying now to default delete all indices of objects for project {}", projectAbbr);
    ArrayList<IndexingReport> indexingReports = new ArrayList<>();
    integrationServices.forEach(integrationService -> {
      indexingReports.add(integrationService.deleteIndexedObjects(projectAbbr));
    });
    return indexingReports;
  }

}
