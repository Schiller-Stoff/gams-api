package org.ddh.gamsapi.application.Integration.BaseSearch;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ddh.gamsapi.application.Integration.BaseSearch.solr.BaseSearchFacetResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.ddh.gamsapi.application.Integration.Common.interfaces.IIntegrationController;
import org.ddh.gamsapi.infrastructure.System.config.OpenAPIConfig;

import java.util.HashMap;
import java.util.List;
import java.util.Set;

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

  // TODO update path
  @GetMapping(path = "/testme", produces = {
      MimeTypeUtils.APPLICATION_JSON_VALUE,
      MimeTypeUtils.APPLICATION_XML_VALUE
  })
  @ResponseBody
  // TODO should i use PagedResponse? and
  public BaseSearchFacetResponse searchDigitalObjectsByDublinCoreAdvanced(
      @RequestParam MultiValueMap<String, String> dcCriteria,
      @RequestParam Set<String> projects,
      @RequestParam(defaultValue = "0") int pageIndex,
      @RequestParam(defaultValue = "20") int pageSize) {

    pageSize = Math.min(pageSize, 20); // Limit page size

    // includes now all request parameters, not just "dc.*" ones
    // only keep parameters keys that start with "dc."
    var filteredDcFields = new HashMap<String, List<String>>();
    dcCriteria.forEach((key, values) -> {
      if (key.startsWith("dc.")) {
        String newKey = key.substring(3); // Remove "dc." prefix
        filteredDcFields.put(newKey, values);
      }
    });

    log.debug("Advanced DC search - criteria: {}, projects: {}",
        dcCriteria, projects);

    return baseSearchService.facetSearch(projects, MultiValueMap.fromMultiValue(filteredDcFields), PageRequest.of(pageIndex, pageSize));
  }

}
