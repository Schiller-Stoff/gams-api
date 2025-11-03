package org.ddh.gamsapi.application.Integration.BaseSearch.Facet;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ddh.gamsapi.application.Integration.BaseSearch.BaseSearchProperties;
import org.ddh.gamsapi.application.Integration.Common.exceptions.IntegrationServiceException;
import org.ddh.gamsapi.domain.Project.interfaces.IProjectService;
import org.ddh.gamsapi.infrastructure.System.config.OpenAPIConfig;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Set;

/**
 * Controller for handling faceted search requests for digital objects.
 */
@Controller
@Slf4j
@RequiredArgsConstructor
@Tag(name = OpenAPIConfig.SEARCH_TAG, description = OpenAPIConfig.SEARCH_TAG_DESCRIPTION)
public class FacetController {

  public static final String FACET_SEARCH_PATH = "/api/v1/integration/gsearch/facets";

  private final FacetService facetService;

  private final IProjectService projectService;

  /**
   * TODO rename method
   * TODO test method?
   * API endpoint for advanced faceted search using Dublin Core metadata.
   * @param dcCriteria All request parameters (will be filtered for dc.* fields)
   * @param projects Selected project abbreviations
   * @param fulltextQuery Optional fulltext search query
   * @param pageIndex Current page (0-indexed)
   * @param pageSize Results per page
   * @param sortBy Sort field (default: dc.title)
   * @param sortDir Sort direction (asc/desc)
   * @return FacetResponseDTO containing search results and facet data
   */
  @GetMapping(path = FACET_SEARCH_PATH, produces = {
      MimeTypeUtils.APPLICATION_JSON_VALUE,
      MimeTypeUtils.APPLICATION_XML_VALUE
  })
  @ResponseBody
  @Operation(
      summary = "Dublin core faceted search based on digital objects and multiple projects.",
      description = "Searches for digital objects based on a faceted search over certain Dublin Core fields. The search is performed on multiple projects and can include multiple Dublin Core fields.",
      responses = {
          @ApiResponse(responseCode = "200", description = "Digital objects found"),
          @ApiResponse(responseCode = "400", description = "Invalid request parameters",
              content = @Content)
      }
  )
  @Parameter(
      name = "format",
      description = "Format of the response. Defaults to JSON.",
      required = false,
      schema = @Schema(type = "string", examples = "xml,json")
  )
  public FacetResponseDTO searchDigitalObjectsByDublinCoreAdvanced(
      @RequestParam MultiValueMap<String, String> dcCriteria,
      @RequestParam Set<String> projects,
      @RequestParam(required = false, defaultValue = "", name = "q") String fulltextQuery,
      @RequestParam(defaultValue = "0") int pageIndex,
      @RequestParam(defaultValue = "20") int pageSize,
      @RequestParam(required = false, defaultValue = "dc.title") String sortBy,
      @RequestParam(required = false, defaultValue = "asc") String sortDir
  ) {

    pageSize = Math.min(pageSize, 50); // Limit page size

    // includes now all request parameters, not just "dc.*" ones
    // only keep parameters keys that start with "dc."
    var filteredDcFields = new HashMap<String, List<String>>();
    dcCriteria.forEach((key, values) -> {
      if (key.startsWith("dc.")) {
        // TODO WTF AM I REMOVING THE dc. PREFIX HERE???
        String newKey = key.substring(3); // Remove "dc." prefix
        filteredDcFields.put(newKey, values);
      }
    });

    log.debug("Advanced DC search - criteria: {}, projects: {}",
        dcCriteria, projects);

    // TODO add tests for sorting procedure!
    PageRequest pageRequest = buildPageRequest(pageIndex, pageSize, sortBy, sortDir);

    return facetService.facetSearch(
        projects,
        fulltextQuery,
        MultiValueMap.fromMultiValue(filteredDcFields),
        pageRequest
    );
  }

  /**
   * HTML view for faceted search interface.
   * Provides an interactive UI for filtering digital objects using Dublin Core facets.
   *
   * @param dcCriteria All request parameters (will be filtered for dc.* fields)
   * @param projects Selected project abbreviations
   * @param fulltextQuery Optional fulltext search query
   * @param pageIndex Current page (0-indexed)
   * @param pageSize Results per page
   * @param sortBy Sort field (default: dc.title)
   * @param sortDir Sort direction (asc/desc)
   * @param model Spring MVC model for template rendering
   * @return Thymeleaf template name
   */
  @GetMapping(path = FACET_SEARCH_PATH, produces = MediaType.TEXT_HTML_VALUE)
  public String searchFacetedHtml(
      @RequestParam MultiValueMap<String, String> dcCriteria,
      @RequestParam(required = false) Set<String> projects,
      @RequestParam(required = false, defaultValue = "", name = "q") String fulltextQuery,
      @RequestParam(defaultValue = "0") int pageIndex,
      @RequestParam(defaultValue = "20") int pageSize,
      @RequestParam(required = false, defaultValue = "dc.title") String sortBy,
      @RequestParam(required = false, defaultValue = "asc") String sortDir,
      Model model) {

    log.debug("Faceted search HTML view - criteria: {}, projects: {}, q: {}",
        dcCriteria, projects, fulltextQuery);

    // If no projects selected, show empty state with all available projects
    if (projects == null || projects.isEmpty()) {
      model.addAttribute("projects", projectService.findAllProjectAbbrs());
      model.addAttribute("searchResults", null);
      model.addAttribute("availableFacets", null);
      model.addAttribute("selectedFacets", new HashMap<>());
      model.addAttribute("totalUnfilteredCount", 0L);
      model.addAttribute("currentQuery", "");
      return "BaseSearch/facets";
    }

    // Limit page size
    pageSize = Math.min(pageSize, 50);

    // Extract only dc.* parameters for filtering
    var filteredDcFields = new HashMap<String, List<String>>();
    dcCriteria.forEach((key, values) -> {
      if (key.startsWith("dc.")) {
        // TODO wtf removing dc. prefix here???
        String newKey = key.substring(3); // Remove "dc." prefix for service layer
        filteredDcFields.put(newKey, values);
      }
    });

    try {
      // Execute faceted search
      Sort sort = sortDir.equalsIgnoreCase("desc")
          ? Sort.by(sortBy).descending()
          : Sort.by(sortBy).ascending();

      FacetResponseDTO response = facetService.facetSearch(
          projects,
          fulltextQuery,
          MultiValueMap.fromMultiValue(filteredDcFields),
          PageRequest.of(pageIndex, pageSize, sort)
      );

      // Add search results and facet data to model
      model.addAttribute("searchResults", response.getResults());
      model.addAttribute("availableFacets", response.getAvailableFacets());
      model.addAttribute("selectedFacets", response.getSelectedFacets());
      model.addAttribute("totalUnfilteredCount", response.getTotalUnfilteredCount());

      // Add all available projects for the project selector
      model.addAttribute("projects", projectService.findAllProjectAbbrs());
      model.addAttribute("selectedProjects", projects);

      // Build query string for pagination links (preserve all current filters)
      String currentQuery = buildQueryString(dcCriteria, projects, fulltextQuery, sortBy, sortDir);
      model.addAttribute("currentQuery", currentQuery);

      // Add search parameters for form repopulation
      model.addAttribute("fulltextQuery", fulltextQuery);
      model.addAttribute("sortBy", sortBy);
      model.addAttribute("sortDir", sortDir);

    } catch (Exception e) {
      log.error("Error executing faceted search", e);
      model.addAttribute("error", "Search failed: " + e.getMessage());
      model.addAttribute("projects", projectService.findAllProjectAbbrs());
      model.addAttribute("searchResults", null);
    }

    return "BaseSearch/facets";
  }


  /**
   * Builds PageRequest with validated sort parameters.
   *
   * @param pageIndex Zero-based page index
   * @param pageSize Number of results per page
   * @param sortBy TODO
   * @param sortDir TODO
   * @return PageRequest with validated sort
   * @throws IllegalArgumentException if sort field is not allowed
   */
  private PageRequest buildPageRequest(int pageIndex, int pageSize, String sortBy, String sortDir) {
    if (sortBy == null || sortBy.isEmpty()) {
      // Default: no explicit sorting (Solr relevance score)
      return PageRequest.of(pageIndex, pageSize);
    }

    String solrField = mapAndValidateSortField(sortBy);

    return PageRequest.of(pageIndex, pageSize, Sort.by(
        Sort.Direction.fromString(sortDir), solrField)
    );

  }

  /**
   * TODO
   *
   *
   */
  private String mapAndValidateSortField(String userField) {

    List<String> allowedSortFields = List.of(
        BaseSearchProperties.TITLE.name,
        BaseSearchProperties.CREATOR.name,
        BaseSearchProperties.PUBLISHER.name,
        BaseSearchProperties.OBJECT_ID.name,
        "dc.title",
        "dc.creator",
        "dc.publisher",
        "dc.type",
        "dc.identifier",
        "dc.date",
        "dc.subject",
        "dc.language",
        "dc.format",
        "dc.rights",
        "dc.coverage",
        "dc.description",
        BaseSearchProperties.TYPE.name,
        BaseSearchProperties.PROJECT.name,
        BaseSearchProperties.DATASTREAMS.name
    );

    if (!allowedSortFields.contains(userField)) {
      String msg = String.format(
          "Sorting by field '%s' is not allowed. Allowed fields are: %s",
          userField,
          String.join(", ", allowedSortFields)
      );
      log.warn(msg);
      // TODO wrong exception - should be a user side error status code!
      throw new IntegrationServiceException(msg);
    }

    return userField;
  }

  /**
  * Builds query string preserving all current filter parameters.
  * Critical for pagination links to maintain filter state.
  *
  * @param dcCriteria All DC filter parameters
   * @param projects Selected projects
   * @param fulltextQuery Fulltext query
   * @param sortBy Sort field
   * @param sortDir Sort direction
   * @return URL-encoded query string
   */
  private String buildQueryString(
      MultiValueMap<String, String> dcCriteria,
      Set<String> projects,
      String fulltextQuery,
      String sortBy,
      String sortDir) {

    UriComponentsBuilder builder = UriComponentsBuilder.newInstance();

    // Add projects
    if (projects != null) {
      projects.forEach(p -> builder.queryParam("projects", p));
    }

    // Add fulltext query if present
    if (fulltextQuery != null && !fulltextQuery.trim().isEmpty()) {
      builder.queryParam("q", fulltextQuery);
    }

    // Add all DC criteria (preserve dc. prefix for URLs)
    dcCriteria.forEach((key, values) -> {
      if (key.startsWith("dc.") || key.equals("q") || key.equals("projects")) {
        // Add each value separately for multi-value support
        values.forEach(value -> builder.queryParam(key, value));
      }
    });

    // Add sort parameters
    builder.queryParam("sortBy", sortBy);
    builder.queryParam("sortDir", sortDir);

    // Return query string without leading '?'
    String query = builder.build().encode().getQuery();
    return query != null ? query : "";
  }








}
