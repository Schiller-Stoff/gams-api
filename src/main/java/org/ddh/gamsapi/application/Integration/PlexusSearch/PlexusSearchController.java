package org.ddh.gamsapi.application.Integration.PlexusSearch;


import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
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

import java.util.List;

@Controller
@RequestMapping
@Slf4j
@RequiredArgsConstructor
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
  @ResponseBody
  public void indexProjectObjects(@PathVariable String projectAbbr){
    log.debug("*** Trying to index project objects");
    plexusSearchService.indexObjects(projectAbbr);
  }

  @Hidden
  @PostMapping(value = PLEXUS_SEARCH_MANAGEMENT_PATH, produces = MediaType.TEXT_HTML_VALUE)
  public String indexProjectObjectsHtml(@PathVariable String projectAbbr){
    log.debug("*** Trying to index project objects");
    plexusSearchService.indexObjects(projectAbbr);
    return "redirect:/api/v1/projects/" + projectAbbr + "/objects";
  }

  @Operation(
      summary = "Add single project object to external plexus-search service",
      description = "This endpoint indexes a single object of a project in the plexus-search service."
  )
  @PostMapping(PLEXUS_SEARCH_SINGLE_OBJECT_MANAGEMENT_PATH)
  @ResponseBody
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
  @ResponseBody
  public void deleteProjectObjects(@PathVariable String projectAbbr){
    log.trace("*** Trying to delete project objects");
    plexusSearchService.deleteIndexedObjects(projectAbbr);
  }

  @Hidden
  @DeleteMapping(value = PLEXUS_SEARCH_MANAGEMENT_PATH, produces =  MediaType.TEXT_HTML_VALUE)
  public String deleteProjectObjectsHtml(@PathVariable String projectAbbr){
    log.trace("*** Trying to delete project objects");
    plexusSearchService.deleteIndexedObjects(projectAbbr);
    return "redirect:/api/v1/projects/" + projectAbbr + "/objects";
  }

  @Operation(
      summary = "Delete single project object from external plexus-search service",
      description = "This endpoint deletes a single object of a project from the plexus-search service."
  )
  @DeleteMapping(PLEXUS_SEARCH_SINGLE_OBJECT_MANAGEMENT_PATH)
  @ResponseBody
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
    log.info("Plexus search request for project: {}, query: {}", project, request);

    PlexusSearchResponseDto response = plexusSearchService.search(project, request);

    return ResponseEntity.ok(response);
  }

  /**
   * Searches the Plexus search index for a specific project using GET parameters.
   * Uses solr parameters and maps them to PlexusSearchQueryRequestDto.
   * @return PlexusSearchResponseDto containing the search results.
   */
  @Operation(
      summary = "Search Plexus indexed project objects",
      description = "This endpoint performs a search query against the Plexus search index for a specific project."
  )
  @GetMapping(
      produces = {
          MimeTypeUtils.APPLICATION_JSON_VALUE,
          MimeTypeUtils.APPLICATION_XML_VALUE
      },
      value = PLEXUS_SEARCH_GET_PATH
  )
  @ResponseBody
  public ResponseEntity<PlexusSearchResponseDto> searchGET(
      @RequestParam(name = "q", required = true) String query,
      @RequestParam(name = "project", required = true) String projectAbbr,
      @RequestParam(name = "start", required = false, defaultValue = "0") Integer start,
      @RequestParam(name = "rows", required = false, defaultValue = "20") Integer rows,
      @RequestParam(name = "sort", required = false, defaultValue = "id desc") String sort,
      @RequestParam(name = "fq", required = false, defaultValue = "") List<String> filterQueries,
      @RequestParam(name = "highlight", required = false, defaultValue = "false") Boolean highlight,
      @RequestParam(name = "highlightFields", required = false, defaultValue = "") List<String> highlightFields,
      @RequestParam(name = "highlightSnippetSize", required = false, defaultValue = "200") Integer highlightSnippetSize,
      @RequestParam(name = "facetFields", required = false) List<String> facetFields,
      @RequestParam(name = "facetLimit", required = false, defaultValue = "10") Integer facetLimit,
      @RequestParam(name = "facetMinCount", required = false, defaultValue = "1") Integer facetMinCount,
      @RequestParam(name = "debug", defaultValue = "false") Boolean debug,
      @RequestParam(name = "fl", required = false, defaultValue = "") List<String> fields,
      @RequestParam(name = "cursorMark", required = false) String cursorMark,
      //@RequestParam Map<String, String> customParams, // TODO custom params atm not supported in GET
      HttpServletRequest request
  ){
    // construct request DTO from GET parameters
    var searchDto = PlexusSearchQueryRequestDto.builder()
        .query(query)
        .start(start)
        .rows(rows)
        .sort(sort)
        .filterQueries(filterQueries)
        .highlight(highlight)
        .highlightFields(highlightFields)
        .highlightSnippetSize(highlightSnippetSize)
        .facetFields(facetFields)
        .facetLimit(facetLimit)
        .facetMinCount(facetMinCount)
        .debug(debug)
        .fields(fields)
        .cursorMark(cursorMark)
        //.customParams(customParams)
        .build();

    log.trace("Mapped GET against {} params to DTO: {}", request.getRequestURI(),  searchDto);

    var responseDto = plexusSearchService.search(
        projectAbbr,
        searchDto
    );

    return ResponseEntity.ok(responseDto);
  }

}
