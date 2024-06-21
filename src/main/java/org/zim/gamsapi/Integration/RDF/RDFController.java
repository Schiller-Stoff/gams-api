package org.zim.gamsapi.Integration.RDF;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.zim.gamsapi.Integration.Common.interfaces.IIntegrationController;

@Controller
@RequestMapping(value = {"/api/v1/integration/projects/{projectAbbr}/objects/rdf", "/api/v1/integration/projects/{projectAbbr}/objects/rdf/"})
@Slf4j
@RequiredArgsConstructor
@RestController
public class RDFController implements IIntegrationController {

  private final RDFService rdfService;

  @Override
  @PostMapping
  public void indexProjectObjects(@PathVariable String projectAbbr) {
    rdfService.indexObjects(projectAbbr);
  }

  @Override
  @DeleteMapping
  public void deleteProjectObjects(@PathVariable String projectAbbr) {
    rdfService.deleteIndexedObjects(projectAbbr);
  }

  @PostMapping("/{id}")
  @Override
  public void indexObject(@PathVariable String projectAbbr, @PathVariable String id){
    rdfService.indexObject(projectAbbr, id);
  }

  @DeleteMapping("/{id}")
  @Override
  public void deleteObject(@PathVariable String projectAbbr, @PathVariable String id) {
    rdfService.deleteIndexedObject(projectAbbr, id);
  }
}
