package org.ddh.gamsapi.application.Integration.CustomSearch;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ddh.gamsapi.application.Integration.Common.exceptions.IntegrationUserQueryException;
import org.ddh.gamsapi.infrastructure.System.config.OpenAPIConfig;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Set;

@Controller
@RequestMapping
@Slf4j
@RequiredArgsConstructor
@Tag(name = OpenAPIConfig.INTEGRATION_TAG, description = OpenAPIConfig.INTEGRATION_TAG_DESCRIPTION)
public class CustomSearchController {

  public static final String CUSTOM_SEARCH_GET_PATH = "/api/v1/integration/c-search";

  public static final String CUSTOM_SEARCH_MANAGEMENT_PATH = CUSTOM_SEARCH_GET_PATH + "/projects/{projectAbbr}/objects";

  public static final String CUSTOM_SEARCH_SINGLE_OBJECT_MANAGEMENT_PATH = CUSTOM_SEARCH_MANAGEMENT_PATH + "/{id}";

  private final CustomSearchService customSearchService;

  @Operation(
      summary = "Add all project objects to external c-search service",
      description = "This endpoint indexes all objects of a project in the c-search service."
  )
  @PostMapping(CUSTOM_SEARCH_MANAGEMENT_PATH)
  @ResponseBody
  public void indexProjectObjects(@PathVariable String projectAbbr){
    log.debug("*** Trying to index project objects");
    customSearchService.indexObjects(projectAbbr);
  }

  @PostMapping(value = CUSTOM_SEARCH_MANAGEMENT_PATH, produces = MimeTypeUtils.TEXT_HTML_VALUE)
  public String indexProjectObjectsHtml(@PathVariable String projectAbbr) {
    log.debug("*** Trying to index project objects for project: {}", projectAbbr);
    customSearchService.indexObjects(projectAbbr);
    return "redirect:/api/v1/projects/" + projectAbbr + "/objects";
  }

  @Operation(
      summary = "Add a single project object to external c-search service",
      description = "This endpoint indexes all objects of a project in the c-search service."
  )
  @PostMapping(CUSTOM_SEARCH_SINGLE_OBJECT_MANAGEMENT_PATH)
  @ResponseBody
  public void indexProjectObject(
      @PathVariable String projectAbbr,
      @PathVariable String id
  ){
    customSearchService.indexObject(projectAbbr, id);
  }

  @Operation(
      summary = "Delete all project objects from external c-search service",
      description = "This endpoint deletes all objects of a project from the CustomSearch service."
  )
  @DeleteMapping(CUSTOM_SEARCH_MANAGEMENT_PATH)
  @ResponseBody
  public void deleteProjectObjects(@PathVariable String projectAbbr){
    log.trace("*** Trying to delete project objects");
    customSearchService.deleteIndexedObjects(projectAbbr);
  }

  @DeleteMapping(value = CUSTOM_SEARCH_MANAGEMENT_PATH, produces = MimeTypeUtils.TEXT_HTML_VALUE)
  public String deleteProjectObjectsHtml(@PathVariable String projectAbbr) {
    log.trace("*** Trying to delete project objects");
    customSearchService.deleteIndexedObjects(projectAbbr);
    return "redirect:/api/v1/projects/" + projectAbbr + "/objects";
  }

  @Operation(
      summary = "Delete a single project object from external c-search service",
      description = "This endpoint deletes a single object of a project from the CustomSearch service."
  )
  @DeleteMapping(CUSTOM_SEARCH_SINGLE_OBJECT_MANAGEMENT_PATH)
  @ResponseBody
  public void deleteProjectObject(
      @PathVariable String projectAbbr,
      @PathVariable String id
  ){
    log.trace("*** Trying to delete single project object from custom-search service. Object-id: {}", id);
    customSearchService.deleteIndexedObject(projectAbbr, id);
  }


  // TODO add missing open api doc
  /**
   * Searches custom entities via fulltext query
   * @param project project to be searched
   * @param fulltextQuery fulltext query
   * @param pageIndex pagination index
   * @param pageSize pagination size
   * @param sortBy sort by field
   * @param sortDir sort direction (desc / asc)
   */
  @GetMapping(produces = {
      MimeTypeUtils.APPLICATION_JSON_VALUE,
      MimeTypeUtils.APPLICATION_XML_VALUE
  }, value = CUSTOM_SEARCH_GET_PATH)
  @ResponseBody
  public CustomSearchResponseDto search(
      @RequestParam String project,
      @RequestParam(required = false, defaultValue = "", name = "q") String fulltextQuery,
      @RequestParam(required = false, defaultValue = "", name = "tag") List<String> tags,
      @RequestParam(required = false, name = "startDate") String startDate,
      @RequestParam(required = false, name = "endDate") String endDate,
      @RequestParam(defaultValue = "0") int pageIndex,
      @RequestParam(defaultValue = "20") int pageSize,
      @RequestParam(required = false, defaultValue = "id") String sortBy,
      @RequestParam(required = false, defaultValue = "asc") String sortDir
      ){

    pageSize = Math.min(pageSize, 50); // Limit page size

    PageRequest pageRequest;

    validateDateFormat(startDate, "startDate");
    validateDateFormat(endDate, "endDate");

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
      Set.of(project),
      tags,
      startDate,
      endDate,
      pageRequest
    );

  }


  // In Controller or Service
  private void validateDateFormat(String date, String paramName) {
    if (date == null || date.isEmpty()) return;

    try {
      Instant.parse(date); // Validates ISO-8601 format
    } catch (DateTimeParseException e) {
      String msg = String.format("Invalid %s format: %s. Must be ISO-8601 with timezone (e.g., 2023-01-01T00:00:00Z)",
          paramName, date);
      log.warn(msg);
      throw new IntegrationUserQueryException(msg);
    }
  }

}
