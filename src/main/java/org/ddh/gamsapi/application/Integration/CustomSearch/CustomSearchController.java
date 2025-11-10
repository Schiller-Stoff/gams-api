package org.ddh.gamsapi.application.Integration.CustomSearch;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ddh.gamsapi.infrastructure.System.config.OpenAPIConfig;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@Controller
@RequestMapping(value = {CustomSearchController.CUSTOM_SEARCH_PATH})
@Slf4j
@RequiredArgsConstructor
@RestController
@Tag(name = OpenAPIConfig.INTEGRATION_TAG, description = OpenAPIConfig.INTEGRATION_TAG_DESCRIPTION)
public class CustomSearchController {

  // TODO rethink endpoint path
  public static final String CUSTOM_SEARCH_PATH = "/api/v1/integration/projects/{projectAbbr}/objects/customSearch";

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


  /**
   * Searches custom entities via fulltext query
   * @param projectAbbr project to be searched
   * @param fulltextQuery fulltext query
   * @param pageIndex pagination index
   * @param pageSize pagination size
   * @param sortBy sort by field
   * @param sortDir sort direction (desc / asc)
   */
  @GetMapping(produces = {
      MimeTypeUtils.APPLICATION_JSON_VALUE,
      MimeTypeUtils.APPLICATION_XML_VALUE
  })
  @ResponseBody
  public CustomSearchResponseDto search(
      @PathVariable String projectAbbr,
      @RequestParam(required = false, defaultValue = "", name = "q") String fulltextQuery,
      @RequestParam(defaultValue = "0") int pageIndex,
      @RequestParam(defaultValue = "20") int pageSize,
      @RequestParam(required = false, defaultValue = "id") String sortBy,
      @RequestParam(required = false, defaultValue = "asc") String sortDir
      ){

    pageSize = Math.min(pageSize, 50); // Limit page size

    PageRequest pageRequest;

    if (sortBy == null || sortBy.isEmpty()) {
      // Default: no explicit sorting (Solr relevance score)
      pageRequest = PageRequest.of(pageIndex, pageSize);
    } else {
      pageRequest = PageRequest.of(pageIndex, pageSize, Sort.by(
          Sort.Direction.fromString(sortDir), sortBy)
      );
    }

    return customSearchService.search(
      fulltextQuery,
      Set.of(projectAbbr),
      pageRequest
    );

  }

}
