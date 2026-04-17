package org.ddh.gamsapi.application.Integration.RDF;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.ddh.gamsapi.application.Integration.Common.interfaces.IIntegrationController;
import org.ddh.gamsapi.infrastructure.System.config.OpenAPIConfig;

@Controller
@RequestMapping(value = {"/api/integration/v1/projects/{projectAbbr}/objects/rdf", "/api/integration/v1/projects/{projectAbbr}/objects/rdf/"})
@Slf4j
@RequiredArgsConstructor
@Tag(name = OpenAPIConfig.INTEGRATION_TAG, description = OpenAPIConfig.INTEGRATION_TAG_DESCRIPTION)
public class RDFController implements IIntegrationController {

  private final RDFService rdfService;

  @Operation(
      summary = "Add all project objects to external RDF service",
      description = "This endpoint adds all objects of a project to the RDF service."
  )
  @Override
  @PostMapping
  @ResponseBody
  public void indexProjectObjects(@PathVariable String projectAbbr) {
    rdfService.indexObjects(projectAbbr);
  }

  @Operation(
      summary = "Delete all project objects from external RDF service",
      description = "This endpoint deletes all objects of a project from the RDF service."
  )
  @Override
  @DeleteMapping
  @ResponseBody
  public void deleteProjectObjects(@PathVariable String projectAbbr) {
    rdfService.deleteIndexedObjects(projectAbbr);
  }

  @Operation(
      summary = "Add a single object to the RDF service",
      description = "This endpoint adds a single object identified by its ID to the RDF service."
  )
  @PostMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  @Override
  public void indexObject(@PathVariable String projectAbbr, @PathVariable String id){
    rdfService.indexObject(projectAbbr, id);
  }

  @Hidden
  @PostMapping(value = "/{id}", produces = MediaType.TEXT_HTML_VALUE)
  public String indexObjectHtml(@PathVariable String projectAbbr, @PathVariable String id) {
    log.debug("*** HTML: Indexing single object {} in RDF for project {}", id, projectAbbr);
    rdfService.indexObject(projectAbbr, id);
    return "redirect:/api/curation/v1/projects/" + projectAbbr + "/objects/" + id;
  }

  @Operation(
      summary = "Delete a single object from the RDF service",
      description = "This endpoint deletes a single object identified by its ID from the RDF service."
  )
  @DeleteMapping(value = "/{id}", produces =  MediaType.APPLICATION_JSON_VALUE)
  @Override
  @ResponseBody
  public void deleteObject(@PathVariable String projectAbbr, @PathVariable String id) {
    rdfService.deleteIndexedObject(projectAbbr, id);
  }

  @Hidden
  @DeleteMapping(value = "/{id}", produces = MediaType.TEXT_HTML_VALUE)
  public String deleteObjectHtml(@PathVariable String projectAbbr, @PathVariable String id) {
    log.debug("*** HTML: Deleting single object {} from RDF for project {}", id, projectAbbr);
    rdfService.deleteIndexedObject(projectAbbr, id);
    return "redirect:/api/curation/v1/projects/" + projectAbbr + "/objects/" + id;
  }
}
