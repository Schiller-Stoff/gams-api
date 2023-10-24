package org.zim.gamsapi.Integration.RDF;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.zim.gamsapi.Integration.IIntegrationController;
import org.zim.gamsapi.Integration.IndexingReport;

import java.util.List;

@Controller
@RequestMapping(value = {"/api/v1/integration/projects/{projectAbbr}/objects/rdf", "/api/v1/integration/projects/{projectAbbr}/objects/rdf/"})
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

  @PostMapping("/{id}")
  @Override
  public List<IndexingReport> indexObject(@PathVariable String projectAbbr, @PathVariable String id){
    return rdfService.indexObject(projectAbbr, id);
  }

  @DeleteMapping("/{id}")
  @Override
  public IndexingReport deleteObject(@PathVariable String projectAbbr, @PathVariable String id) {
    return rdfService.deleteIndexedObject(projectAbbr, id);
  }
}
