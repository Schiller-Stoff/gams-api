package org.ddh.gamsapi.application.Integration.Common;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ddh.gamsapi.application.Integration.Common.interfaces.ClientManagedIntegrationService;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.ddh.gamsapi.infrastructure.System.config.OpenAPIConfig;
import java.util.List;

@Controller
@RequestMapping(value = { "/api/integration/v1/projects/{projectAbbr}/objects" })
@Slf4j
@RequiredArgsConstructor
@Tag(name = OpenAPIConfig.INTEGRATION_TAG, description = OpenAPIConfig.INTEGRATION_TAG_DESCRIPTION)
public class IntegrationActionController {

  /**
   * All beans injected that implement the interface.
   */
  private final List<ClientManagedIntegrationService> integrationServices;

  @Operation(
      summary = "Integrate digital object with external services",
      description = "This endpoint allows to index, delete or re-index objects in external systems. ",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Object integrated successfully"
          ),
          @ApiResponse(
              responseCode = "404",
              description = "Project or object not found"
          ),
          @ApiResponse(
              responseCode = "500",
              description = "Internal server error"
          )
      }
  )
  @ResponseBody
  @PostMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
  public void indexProjectObject(@PathVariable String projectAbbr, @PathVariable String id){
    log.trace("*** Trying now to default index object {} for project {}", id, projectAbbr);
    integrationServices.forEach(integrationService -> integrationService.indexObject(projectAbbr, id));

  }

  @Hidden
  @PostMapping(value = "/{id}", produces = MediaType.TEXT_HTML_VALUE)
  public String indexProjectObjectHtml(@PathVariable String projectAbbr, @PathVariable String id) {
    log.trace("*** HTML: Default index object {} for project {}", id, projectAbbr);
    integrationServices.forEach(service -> service.indexObject(projectAbbr, id));
    return "redirect:/api/curation/v1/projects/" + projectAbbr + "/objects/" + id;
  }

  @Operation(
      summary = "Delete digital object from external services",
      description = "This endpoint allows to delete an object from external systems.",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Object deleted successfully"
          ),
          @ApiResponse(
              responseCode = "404",
              description = "Project or object not found"
          ),
          @ApiResponse(
              responseCode = "500",
              description = "Internal server error"
          )
      }
  )
  @DeleteMapping(value = "/{id}", produces =  MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  public void deleteProjectObject(@PathVariable String projectAbbr, @PathVariable String id){
    log.trace("*** Trying now to default delete object {} for project {}", id, projectAbbr);
    integrationServices.forEach(integrationService -> integrationService.deleteIndexedObject(projectAbbr, id));
  }

  @Hidden
  @DeleteMapping(value = "/{id}", produces = MediaType.TEXT_HTML_VALUE)
  public String deleteProjectObjectHtml(@PathVariable String projectAbbr, @PathVariable String id) {
    log.trace("*** HTML: Default delete object {} for project {}", id, projectAbbr);
    integrationServices.forEach(service -> service.deleteIndexedObject(projectAbbr, id));
    return "redirect:/api/curation/v1/projects/" + projectAbbr + "/objects/" + id;
  }

  @Operation(
      summary = "Add all project's digital objects to external services",
      description = "This endpoint allows to integrate all objects for a project in external systems.",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Objects integrated successfully"
          ),
          @ApiResponse(
              responseCode = "404",
              description = "Project not found"
          ),
          @ApiResponse(
              responseCode = "500",
              description = "Internal server error"
          )
      }
  )
  @PostMapping
  @ResponseBody
  public void indexProjectObjects(@PathVariable String projectAbbr){
    log.trace("*** Trying now to default index all objects for project {}", projectAbbr);
    integrationServices.forEach(integrationService -> integrationService.indexObjects(projectAbbr));
  }

  @Operation(
      summary = "Delete all digital object from external services for a project",
      description = "This endpoint allows to delete all digital object integrations for a project in external systems.",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Objects integrations removed successfully"
          ),
          @ApiResponse(
              responseCode = "404",
              description = "Project not found"
          ),
          @ApiResponse(
              responseCode = "500",
              description = "Internal server error"
          )
      }
  )
  @DeleteMapping
  @ResponseBody
  public void deleteProjectIndices(@PathVariable String projectAbbr){
    log.trace("*** Trying now to default delete all indices of objects for project {}", projectAbbr);
    integrationServices.forEach(integrationService -> integrationService.deleteIndexedObjects(projectAbbr));
  }

}
