package org.ddh.gamsapi.application.Integration.CustomSearch;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ddh.gamsapi.infrastructure.System.config.OpenAPIConfig;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping(value = {"/api/v1/integration/projects/{projectAbbr}/objects/customSearch"})
@Slf4j
@RequiredArgsConstructor
@RestController
@Tag(name = OpenAPIConfig.INTEGRATION_TAG, description = OpenAPIConfig.INTEGRATION_TAG_DESCRIPTION)
public class CustomSearchController {

  private final CustomSearchService customSearchService;

  @Operation(
      summary = "Add all project objects to external CustomSearch service",
      description = "This endpoint indexes all objects of a project in the CustomSearch service."
  )
  @PostMapping
  public void indexProjectObjects(@PathVariable String projectAbbr){
    log.debug("*** Trying to index project objects");
    customSearchService.indexObjects(projectAbbr);
  }

  @Operation(
      summary = "Delete all project objects from external CustomSearch service",
      description = "This endpoint deletes all objects of a project from the CustomSearch service."
  )
  @DeleteMapping
  public void deleteProjectObjects(@PathVariable String projectAbbr){
    log.trace("*** Trying to delete project objects");
    customSearchService.deleteIndexedObjects(projectAbbr);
  }

}
