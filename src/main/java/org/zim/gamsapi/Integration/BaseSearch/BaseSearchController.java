package org.zim.gamsapi.Integration.BaseSearch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.zim.gamsapi.Integration.Common.interfaces.IIntegrationController;

@Controller
@RequestMapping(value = {"/api/v1/integration/projects/{projectAbbr}/objects/search", "/api/v1/integration/projects/{projectAbbr}/objects/search/"})
@Slf4j
@RequiredArgsConstructor
@RestController
public class BaseSearchController implements IIntegrationController {

  private final BaseSearchService baseSearchService;

  @PostMapping
  public void indexProjectObjects(@PathVariable String projectAbbr){
    log.debug("*** Trying to index project objects");
    baseSearchService.indexObjects(projectAbbr);
  }

  @DeleteMapping
  public void deleteProjectObjects(@PathVariable String projectAbbr){
    log.trace("*** Trying to delete project objects");
    baseSearchService.deleteIndexedObjects(projectAbbr);
  }

  @PostMapping("/{pid}")
  public void indexObject(@PathVariable String projectAbbr, @PathVariable String pid){
    log.trace("*** Trying to index object with pid {}", pid);
    baseSearchService.indexObject(projectAbbr, pid);
  }

  @DeleteMapping("/{pid}")
  public void deleteObject(@PathVariable String projectAbbr, @PathVariable String pid){
    log.trace("*** Trying to delete object with pid {}", pid);
    baseSearchService.deleteIndexedObject(projectAbbr, pid);
  }

  @PostMapping("/setup")
  public void setupIntegrationService(@PathVariable String projectAbbr){
    log.trace("*** Setting up integration service {}", this.getClass().getSimpleName());
    baseSearchService.setupIntegrationService(projectAbbr);
  }

}
