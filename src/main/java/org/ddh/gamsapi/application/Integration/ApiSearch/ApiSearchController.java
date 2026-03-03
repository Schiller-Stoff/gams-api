package org.ddh.gamsapi.application.Integration.ApiSearch;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ddh.gamsapi.application.Integration.Common.interfaces.IIntegrationController;
import org.ddh.gamsapi.infrastructure.System.config.OpenAPIConfig;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping(value = {"/api/v1/integration/projects/{projectAbbr}/objects/search"})
@Slf4j
@RequiredArgsConstructor
@Tag(name = OpenAPIConfig.INTEGRATION_TAG, description = OpenAPIConfig.INTEGRATION_TAG_DESCRIPTION)
public class ApiSearchController implements IIntegrationController {

  public static final String API_SEARCH_MANAGEMENT_PATH = "/api/v1/integration/projects/{projectAbbr}/objects/search";
  public static final String API_SEARCH_SINGLE_OBJECT_MANAGEMENT_PATH = API_SEARCH_MANAGEMENT_PATH + "/{pid}";

  private final ApiSearchService apiSearchService;

  @Operation(
      summary = "Add all project objects to Api-Search service",
      description = "This endpoint indexes all objects of a project in the Api-Search service."
  )
  @PostMapping
  @ResponseBody
  public void indexProjectObjects(@PathVariable String projectAbbr){
    log.debug("*** Trying to index project objects");
    apiSearchService.indexObjects(projectAbbr);
  }

  @Operation(
      summary = "Delete all project objects from external Api-Search service",
      description = "This endpoint deletes all objects of a project from the Api-Search service."
  )
  @DeleteMapping
  @ResponseBody
  public void deleteProjectObjects(@PathVariable String projectAbbr){
    log.trace("*** Trying to delete project objects");
    apiSearchService.deleteIndexedObjects(projectAbbr);
  }

  @Operation(
      summary = "Add a single object to the Api-Search service",
      description = "This endpoint indexes a single object identified by its PID in the Api-Search service."
  )
  @PostMapping(value = "/{pid}", produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  public void indexObject(@PathVariable String projectAbbr, @PathVariable String pid){
    log.trace("*** Trying to index object with pid {}", pid);
    apiSearchService.indexObject(projectAbbr, pid);
  }

  @Hidden
  @PostMapping(value = "/{pid}", produces = MediaType.TEXT_HTML_VALUE)
  public String indexObjectHtml(@PathVariable String projectAbbr, @PathVariable String pid) {
    log.debug("*** HTML: Indexing single object {} in api-search for project {}", pid, projectAbbr);
    apiSearchService.indexObject(projectAbbr, pid);
    return "redirect:/api/v1/projects/" + projectAbbr + "/objects/" + pid;
  }

  @Operation(
      summary = "Delete a single object from the Api-Search service",
      description = "This endpoint deletes a single object identified by its PID from the Api-Search service."
  )
  @DeleteMapping("/{pid}")
  @ResponseBody
  public void deleteObject(@PathVariable String projectAbbr, @PathVariable String pid){
    log.trace("*** Trying to delete object with pid {}", pid);
    apiSearchService.deleteIndexedObject(projectAbbr, pid);
  }

  @Hidden
  @DeleteMapping(value = "/{pid}", produces = MediaType.TEXT_HTML_VALUE)
  public String deleteObjectHtml(@PathVariable String projectAbbr, @PathVariable String pid) {
    log.debug("*** HTML: Deleting single object {} from api-search for project {}", pid, projectAbbr);
    apiSearchService.deleteIndexedObject(projectAbbr, pid);
    return "redirect:/api/v1/projects/" + projectAbbr + "/objects/" + pid;
  }

}
