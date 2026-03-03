package org.ddh.gamsapi.application.Integration.ApiSearch.Facet;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ddh.gamsapi.application.Integration.ApiSearch.ApiSearchProperties;
import org.ddh.gamsapi.application.Integration.Common.exceptions.IntegrationUserQueryException;
import org.ddh.gamsapi.domain.Project.interfaces.IProjectService;
import org.ddh.gamsapi.infrastructure.System.config.OpenAPIConfig;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.LinkedMultiValueMap;
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

  public static final String FACET_SEARCH_PATH = "/api/v1/integration/search/facets";

  private final FacetService facetService;

  private final IProjectService projectService;

  /**
   * API endpoint for advanced faceted search using Dublin Core metadata.
   * @param allRequestParams All request parameters (will be filtered for dc.* fields)
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
  public FacetResponseDTO facetSearch(
      @RequestParam MultiValueMap<String, String> allRequestParams,
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
    var filteredDcFields = extractAndValidateDcCriteria(allRequestParams);

    log.debug("Advanced DC search - criteria: {}, projects: {}",
        filteredDcFields, projects);

    // TODO add tests for sorting procedure!
    PageRequest pageRequest = buildPageRequest(pageIndex, pageSize, sortBy, sortDir);

    return facetService.facetSearch(
        projects,
        fulltextQuery,
        filteredDcFields,
        pageRequest
    );
  }

  /**
   * HTML view for faceted search interface.
   * Provides an interactive UI for filtering digital objects using Dublin Core facets.
   *
   * @param allRequestParams All request parameters (will be filtered for dc.* fields)
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
      @RequestParam MultiValueMap<String, String> allRequestParams,
      @RequestParam(required = false) Set<String> projects,
      @RequestParam(required = false, defaultValue = "", name = "q") String fulltextQuery,
      @RequestParam(defaultValue = "0") int pageIndex,
      @RequestParam(defaultValue = "20") int pageSize,
      @RequestParam(required = false, defaultValue = "dc.title") String sortBy,
      @RequestParam(required = false, defaultValue = "asc") String sortDir,
      Model model) {

    log.debug("Faceted search HTML view - criteria: {}, projects: {}, q: {}",
        allRequestParams, projects, fulltextQuery);

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
    var filteredDcFields = extractAndValidateDcCriteria(allRequestParams);

    try {
      // Execute faceted search
      Sort sort = sortDir.equalsIgnoreCase("desc")
          ? Sort.by(sortBy).descending()
          : Sort.by(sortBy).ascending();

      FacetResponseDTO response = facetService.facetSearch(
          projects,
          fulltextQuery,
          filteredDcFields,
          PageRequest.of(pageIndex, pageSize, sort)
      );

      // Add search results and facet data to model
      model.addAttribute("searchResults", response.getResult());
      model.addAttribute("availableFacets", response.getAvailableFacets());
      model.addAttribute("selectedFacets", response.getSelectedFacets());
      model.addAttribute("totalUnfilteredCount", response.getTotalUnfilteredCount());

      // Add all available projects for the project selector
      model.addAttribute("projects", projectService.findAllProjectAbbrs());
      model.addAttribute("selectedProjects", projects);

      // Build query string for pagination links (preserve all current filters)
      String currentQuery = buildQueryString(allRequestParams, projects, fulltextQuery, sortBy, sortDir);
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
   * @param sortBy Sort field
   * @param sortDir Sort direction (asc/desc)
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
   * Extracts and validates DC criteria from all request parameters.
   * Only allows certain DC fields for faceting.
   * @param allParams All request parameters
   * @return Filtered DC criteria
   * @throws IntegrationUserQueryException if any disallowed DC field is present
   */
  public MultiValueMap<String, String> extractAndValidateDcCriteria(MultiValueMap<String, String> allParams) {
    MultiValueMap<String, String> dcCriteria = new LinkedMultiValueMap<>();

    final List<String> ALLOWED_DC_FIELDS = List.of(
        "dc.subject",
        "dc.coverage",
        "dc.rights",
        "dc.type",
        "dc.format",
        "dc.language"
    );

    allParams.forEach((key, values) -> {
      if (key.startsWith("dc.")) {
        if(!ALLOWED_DC_FIELDS.contains(key)){
          String msg = String.format(
              "Faceting by dc field '%s' is not allowed. Allowed fields are: %s",
              key,
              String.join(", ", ALLOWED_DC_FIELDS)
          );
          log.warn(msg);
          throw new IntegrationUserQueryException(msg);
        }
        dcCriteria.put(key, values);
      }
    });

    return dcCriteria;
  }


  /**
   * Maps user-provided sort field to Solr field and validates it.
   * @param userField User-provided sort field
   * @return Mapped Solr field
   * @throws IntegrationUserQueryException if field is not allowed for sorting
   */
  private String mapAndValidateSortField(String userField) {

    List<String> allowedSortFields = List.of(
        ApiSearchProperties.TITLE.name,
        ApiSearchProperties.CREATOR.name,
        ApiSearchProperties.PUBLISHER.name,
        ApiSearchProperties.OBJECT_ID.name,
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
        ApiSearchProperties.TYPE.name,
        ApiSearchProperties.PROJECT.name,
        ApiSearchProperties.DATASTREAMS.name
    );

    if (!allowedSortFields.contains(userField)) {
      String msg = String.format(
          "Sorting by field '%s' is not allowed. Allowed fields are: %s",
          userField,
          String.join(", ", allowedSortFields)
      );
      log.warn(msg);
      throw new IntegrationUserQueryException(msg);
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
