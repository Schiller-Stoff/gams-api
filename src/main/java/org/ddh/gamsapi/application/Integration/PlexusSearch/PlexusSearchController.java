package org.ddh.gamsapi.application.Integration.PlexusSearch;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ddh.gamsapi.application.Integration.PlexusSearch.dto.PlexusSearchQueryRequestDto;
import org.ddh.gamsapi.application.Integration.PlexusSearch.dto.PlexusSearchResponseDto;
import org.ddh.gamsapi.infrastructure.System.config.OpenAPIConfig;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping
@Slf4j
@RequiredArgsConstructor
@RestController
@Tag(name = OpenAPIConfig.INTEGRATION_TAG, description = OpenAPIConfig.INTEGRATION_TAG_DESCRIPTION)
public class PlexusSearchController {

  public static final String PLEXUS_SEARCH_GET_PATH = "/api/v1/integration/plexus-search";

  public static final String PLEXUS_SEARCH_MANAGEMENT_PATH = PLEXUS_SEARCH_GET_PATH + "/projects/{projectAbbr}/objects";

  public static final String PLEXUS_SEARCH_SINGLE_OBJECT_MANAGEMENT_PATH = PLEXUS_SEARCH_MANAGEMENT_PATH + "/{id}";

  private final PlexusSearchService plexusSearchService;

  @Operation(
      summary = "Add all project objects to external plexus-search service",
      description = "This endpoint indexes all objects of a project in the plexus-search service."
  )
  @PostMapping(PLEXUS_SEARCH_MANAGEMENT_PATH)
  public void indexProjectObjects(@PathVariable String projectAbbr){
    log.debug("*** Trying to index project objects");
    plexusSearchService.indexObjects(projectAbbr);
  }


  @Operation(
      summary = "Delete all project objects from external plexus-search service",
      description = "This endpoint deletes all objects of a project from the plexus-search service."
  )
  @DeleteMapping(PLEXUS_SEARCH_MANAGEMENT_PATH)
  public void deleteProjectObjects(@PathVariable String projectAbbr){
    log.trace("*** Trying to delete project objects");
    plexusSearchService.deleteIndexedObjects(projectAbbr);
  }


  /**
   * TODO jdoc
   * TODO test
   * TODO openapi
   * @param projectAbbr
   * @param request
   * @return
   */
  public ResponseEntity<PlexusSearchResponseDto> search(
      @Parameter(description = "Project abbreviation", required = true)
      @PathVariable String projectAbbr,

      @Parameter(description = "Query parameters", required = true)
      @Valid @RequestBody PlexusSearchQueryRequestDto request
  ) {
    log.info("Plexus search request for project: {}, query: {}", projectAbbr, request.getQuery());

    PlexusSearchResponseDto response = plexusSearchService.search(projectAbbr, request);

    return ResponseEntity.ok(response);
  }

}
