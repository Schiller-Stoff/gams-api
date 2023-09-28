package org.zim.gamsapi.Integration.RDF;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.zim.gamsintegrationapi.IIntegrationController;
import org.zim.gamsintegrationapi.IndexingReport;

import java.util.List;

@Controller
@RequestMapping(value = {"/api/v1/integration/projects/{projectAbbr}/rdf", "/api/v1/integration/projects/{projectAbbr}/rdf/"})
@Slf4j
@RequiredArgsConstructor
@RestController
public class RDFController implements IIntegrationController {

  private final RDFService rdfService;

  @Override
  @PostMapping
  public List<IndexingReport> indexProjectObjects(@PathVariable String projectAbbr) {
    return rdfService.indexObjects(projectAbbr);
  }

  @Override
  @DeleteMapping
  public IndexingReport deleteProjectObjects(@PathVariable String projectAbbr) {
    return rdfService.deleteIndexedObjects(projectAbbr);
  }

  @PostMapping("/objects/{pid}")
  @Override
  public List<IndexingReport> indexObject(@PathVariable String projectAbbr, @PathVariable String pid){
    return rdfService.indexObject(projectAbbr, pid);
  }

  @DeleteMapping("/objects/{pid}")
  @Override
  public IndexingReport deleteObject(@PathVariable String projectAbbr, @PathVariable String pid) {
    return rdfService.deleteIndexedObject(projectAbbr, pid);
  }
}
