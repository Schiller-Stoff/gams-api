package org.zim.gamsapi.Integration.BaseSearch;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.zim.gamsapi.Integration.Common.interfaces.IIntegrationController;
import org.zim.gamsapi.System.config.OpenAPIConfig;

@Controller
@RequestMapping(value = {"/api/v1/integration/projects/{projectAbbr}/objects/search"})
@Slf4j
@RequiredArgsConstructor
@RestController
@Tag(name = OpenAPIConfig.INTEGRATION_TAG, description = OpenAPIConfig.INTEGRATION_TAG_DESCRIPTION)
public class BaseSearchController implements IIntegrationController {

  private final BaseSearchService baseSearchService;

  @Operation(
      summary = "Add all project objects to external BaseSearch service",
      description = "This endpoint indexes all objects of a project in the BaseSearch service."
  )
  @PostMapping
  public void indexProjectObjects(@PathVariable String projectAbbr){
    log.debug("*** Trying to index project objects");
    baseSearchService.indexObjects(projectAbbr);
  }

  @Operation(
      summary = "Delete all project objects from external BaseSearch service",
      description = "This endpoint deletes all objects of a project from the BaseSearch service."
  )
  @DeleteMapping
  public void deleteProjectObjects(@PathVariable String projectAbbr){
    log.trace("*** Trying to delete project objects");
    baseSearchService.deleteIndexedObjects(projectAbbr);
  }

  @Operation(
      summary = "Add a single object to the BaseSearch service",
      description = "This endpoint indexes a single object identified by its PID in the BaseSearch service."
  )
  @PostMapping("/{pid}")
  public void indexObject(@PathVariable String projectAbbr, @PathVariable String pid){
    log.trace("*** Trying to index object with pid {}", pid);
    baseSearchService.indexObject(projectAbbr, pid);
  }

  @Operation(
      summary = "Delete a single object from the BaseSearch service",
      description = "This endpoint deletes a single object identified by its PID from the BaseSearch service."
  )
  @DeleteMapping("/{pid}")
  public void deleteObject(@PathVariable String projectAbbr, @PathVariable String pid){
    log.trace("*** Trying to delete object with pid {}", pid);
    baseSearchService.deleteIndexedObject(projectAbbr, pid);
  }

  @Operation(
      summary = "Setup project specific BaseSearch integration service",
      description = "This endpoint sets up the BaseSearch integration service for a project. " +
          "It should be called once per project to initialize the integration."
  )
  @PostMapping("/setup")
  public void setupIntegrationService(@PathVariable String projectAbbr){
    log.trace("*** Setting up integration service {}", this.getClass().getSimpleName());
    baseSearchService.setupIntegrationService(projectAbbr);
  }

}
