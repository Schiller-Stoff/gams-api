package org.zim.gamsapi.application.Integration.RDF;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.zim.gamsapi.application.Integration.Common.interfaces.IIntegrationController;
import org.zim.gamsapi.infrastructure.System.config.OpenAPIConfig;

@Controller
@RequestMapping(value = {"/api/v1/integration/projects/{projectAbbr}/objects/rdf", "/api/v1/integration/projects/{projectAbbr}/objects/rdf/"})
@Slf4j
@RequiredArgsConstructor
@RestController
@Tag(name = OpenAPIConfig.INTEGRATION_TAG, description = OpenAPIConfig.INTEGRATION_TAG_DESCRIPTION)
public class RDFController implements IIntegrationController {

  private final RDFService rdfService;

  @Operation(
      summary = "Add all project objects to external RDF service",
      description = "This endpoint adds all objects of a project to the RDF service."
  )
  @Override
  @PostMapping
  public void indexProjectObjects(@PathVariable String projectAbbr) {
    rdfService.indexObjects(projectAbbr);
  }

  @Operation(
      summary = "Delete all project objects from external RDF service",
      description = "This endpoint deletes all objects of a project from the RDF service."
  )
  @Override
  @DeleteMapping
  public void deleteProjectObjects(@PathVariable String projectAbbr) {
    rdfService.deleteIndexedObjects(projectAbbr);
  }

  @Operation(
      summary = "Add a single object to the RDF service",
      description = "This endpoint adds a single object identified by its ID to the RDF service."
  )
  @PostMapping("/{id}")
  @Override
  public void indexObject(@PathVariable String projectAbbr, @PathVariable String id){
    rdfService.indexObject(projectAbbr, id);
  }

  @Operation(
      summary = "Delete a single object from the RDF service",
      description = "This endpoint deletes a single object identified by its ID from the RDF service."
  )
  @DeleteMapping("/{id}")
  @Override
  public void deleteObject(@PathVariable String projectAbbr, @PathVariable String id) {
    rdfService.deleteIndexedObject(projectAbbr, id);
  }
}
