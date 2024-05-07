package org.zim.gamsapi.Integration.BaseSearch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.zim.gamsapi.Integration.Common.interfaces.IIntegrationController;
import org.zim.gamsapi.Integration.Common.IntegrationActionReport;

import java.util.List;

@Controller
@RequestMapping(value = {"/api/v1/integration/projects/{projectAbbr}/objects/search", "/api/v1/integration/projects/{projectAbbr}/objects/search/"})
@Slf4j
@RequiredArgsConstructor
@RestController
public class BaseSearchController implements IIntegrationController {

  private final BaseSearchService baseSearchService;

  @PostMapping
  public List<IntegrationActionReport> indexProjectObjects(@PathVariable String projectAbbr){
    log.debug("*** Trying to index project objects");
    return baseSearchService.indexObjects(projectAbbr);
  }

  @DeleteMapping
  public List<IntegrationActionReport> deleteProjectObjects(@PathVariable String projectAbbr){
    log.trace("*** Trying to delete project objects");
    return baseSearchService.deleteIndexedObjects(projectAbbr);
  }

  @PostMapping("/{pid}")
  public List<IntegrationActionReport> indexObject(@PathVariable String projectAbbr, @PathVariable String pid){
    log.trace("*** Trying to index object with pid {}", pid);
    return baseSearchService.indexObject(projectAbbr, pid);
  }

  @DeleteMapping("/{pid}")
  public List<IntegrationActionReport> deleteObject(@PathVariable String projectAbbr, @PathVariable String pid){
    log.trace("*** Trying to delete object with pid {}", pid);
    return baseSearchService.deleteIndexedObject(projectAbbr, pid);
  }

}
