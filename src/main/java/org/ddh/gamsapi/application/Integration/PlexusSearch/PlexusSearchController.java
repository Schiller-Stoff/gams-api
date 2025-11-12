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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.MimeTypeUtils;
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
      summary = "Add single project object to external plexus-search service",
      description = "This endpoint indexes a single object of a project in the plexus-search service."
  )
  @PostMapping(PLEXUS_SEARCH_SINGLE_OBJECT_MANAGEMENT_PATH)
  public void indexProjectObject(
      @PathVariable String projectAbbr,
      @PathVariable String id
  ){
    log.debug("*** Trying to index single project object");
    plexusSearchService.indexObject(projectAbbr, id);
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

  @Operation(
      summary = "Delete single project object from external plexus-search service",
      description = "This endpoint deletes a single object of a project from the plexus-search service."
  )
  @DeleteMapping(PLEXUS_SEARCH_SINGLE_OBJECT_MANAGEMENT_PATH)
  public void deleteProjectObject(
      @PathVariable String projectAbbr,
      @PathVariable String id
  ){
    log.trace("*** Trying to delete single project object");
    plexusSearchService.deleteIndexedObject(projectAbbr, id);
  }


  /**
   * TODO jdoc
   * TODO test
   * TODO improve openapi doc
   * @param project
   * @param request
   * @return
   */
  @Operation(
      summary = "Search Plexus indexed project objects",
      description = "This endpoint performs a search query against the Plexus search index for a specific project."
  )
  @PostMapping(
      produces = {
        MimeTypeUtils.APPLICATION_JSON_VALUE,
        MimeTypeUtils.APPLICATION_XML_VALUE
      },
      value = PLEXUS_SEARCH_GET_PATH,
      consumes = {
        MediaType.APPLICATION_FORM_URLENCODED_VALUE,
        MediaType.APPLICATION_JSON_VALUE,
        "application/x-www-form-urlencoded;charset=UTF-8"
      }
  )
  @ResponseBody
  public ResponseEntity<PlexusSearchResponseDto> search(
      @Parameter(description = "Project abbreviation", required = true)
      @RequestParam String project,
      @Parameter(description = "Query parameters", required = true)
      @Valid @RequestBody PlexusSearchQueryRequestDto request
  ) {
    log.info("Plexus search request for project: {}, query: {}", project, request.getQuery());

    PlexusSearchResponseDto response = plexusSearchService.search(project, request);

    return ResponseEntity.ok(response);
  }

}
