package org.zim.gamsapi.Integration.Facet;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.zim.gamsintegrationapi.IndexingReport;

import java.util.List;

@Controller
@RequestMapping(value = {"/api/v1/integration/projects/{projectAbbr}/facets", "/api/v1/integration/projects/{projectAbbr}/facets/"})
@Slf4j
@RequiredArgsConstructor
@RestController
public class FacetController {

  private final FacetService facetService;

  @PostMapping
  public List<IndexingReport> indexProjectObjects(@PathVariable String projectAbbr){
    log.debug("*** Trying to index project objects");
    return facetService.indexObjects(projectAbbr);
  }

  @DeleteMapping
  public IndexingReport deleteProjectObjects(@PathVariable String projectAbbr){
    log.trace("*** Trying to delete project objects");
    return facetService.deleteIndexedObjects(projectAbbr);
  }

  @PostMapping("/objects/{pid}")
  public List<IndexingReport> indexObject(@PathVariable String projectAbbr, @PathVariable String pid){
    log.trace("*** Trying to index object with pid {}", pid);
    return facetService.indexObject(projectAbbr, pid);
  }

  @DeleteMapping("/objects/{pid}")
  public IndexingReport deleteObject(@PathVariable String projectAbbr, @PathVariable String pid){
    log.trace("*** Trying to delete object with pid {}", pid);
    return facetService.deleteIndexedObject(projectAbbr, pid);
  }

}
