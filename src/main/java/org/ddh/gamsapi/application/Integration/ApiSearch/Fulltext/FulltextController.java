package org.ddh.gamsapi.application.Integration.ApiSearch.Fulltext;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ddh.gamsapi.application.Integration.ApiSearch.ApiSearchProperties;
import org.ddh.gamsapi.domain.Project.interfaces.IProjectService;
import org.ddh.gamsapi.infrastructure.System.config.OpenAPIConfig;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
 * Controller for handling fulltext search requests for digital objects.
 */
@Controller
@Slf4j
@RequiredArgsConstructor
@Tag(name = OpenAPIConfig.SEARCH_TAG, description = OpenAPIConfig.SEARCH_TAG_DESCRIPTION)
public class FulltextController {

  public static final String FULLTEXT_SEARCH_PATH = "/api/v1/integration/search/fulltext";

  private final FulltextService fulltextService;

  private final IProjectService projectService;


  /**
   * Fulltext search for digital objects with Dublin Core criteria.
   * @param dcCriteria Dublin Core search criteria
   * @param projects Selected projects
   * @param fulltextQuery Fulltext search query
   * @param pageIndex Current page index
   * @param pageSize Number of results per page
   * @param sortBy Sort field
   * @param sortDir Sort direction
   * @return Paginated search results
   */
  @GetMapping(path = FULLTEXT_SEARCH_PATH, produces = {
      MimeTypeUtils.APPLICATION_JSON_VALUE,
      MimeTypeUtils.APPLICATION_XML_VALUE
  })
  @ResponseBody
  @Operation(
      summary = "Fulltext search based on digital objects and multiple projects.",
      description = "Searches for digital objects based on fulltext. Combinable with dublin core arguments. The search is performed on multiple projects and can include multiple Dublin Core fields.",
      responses = {
          @ApiResponse(responseCode = "200", description = "Digital objects found"),
          @ApiResponse(responseCode = "400", description = "Invalid request parameters",
              content = @Content)
      }
  )
  @Parameter(
      name = "q",
      description = "Fulltext search query.",
      required = false,
      schema = @Schema(type = "string", example = "example search term")
  )
  @Parameter(
      name = "projects",
      description = "List of project abbreviations to search within. If not provided, all projects are searched.",
      required = false,
      schema = @Schema(type = "array", example = "[\"project1\", \"project2\"]")
  )
  @Parameter(
      name = "dc.*",
      description = "Dublin Core search criteria. Use 'dc.' prefix followed by the field name (e.g., dc.title, dc.creator). Multiple values can be provided for each field.",
      required = false,
      schema = @Schema(type = "string", examples = "dc.title=Example Title,dc.creatorAsPhrase=John Doe")
  )
  @Parameter(
      name = "format",
      description = "Format of the response. Defaults to JSON.",
      required = false,
      schema = @Schema(type = "string", examples = "xml,json")
  )
  public FulltextDigitalObjectResultDto searchDigitalObjects(
      @RequestParam MultiValueMap<String, String> dcCriteria,
      @RequestParam(required = false, defaultValue = "") Set<String> projects,
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
        filteredDcFields.put(key, values);
      }
    });

    PageRequest pageRequest = buildPageRequest(pageIndex, pageSize, sortBy, sortDir);

    return fulltextService.search(
        fulltextQuery,
        filteredDcFields,
        projects,
        pageRequest
    );

  }

  /**
   * Webclient view for the  fulltext search for digital objects with Dublin Core criteria.
   * TODO OpenAPI doc
   *
   * @param dcCriteria Dublin Core search criteria
   * @param projects Selected projects
   * @param fulltextQuery Fulltext search query
   * @param pageIndex Current page index
   * @param pageSize Number of results per page
   * @param sortBy field to be sorted by
   * @param sortDir sort direction
   * @return HTML view with paginated search results
   */
  @GetMapping(path = FULLTEXT_SEARCH_PATH, produces = MimeTypeUtils.TEXT_HTML_VALUE)
  public String searchDigitalObjectsByWebView(
      @RequestParam MultiValueMap<String, String> dcCriteria,
      @RequestParam(required = false, defaultValue = "") Set<String> projects,
      @RequestParam(required = false, defaultValue = "", name = "q") String fulltextQuery,
      @RequestParam(defaultValue = "0") int pageIndex,
      @RequestParam(defaultValue = "20") int pageSize,
      @RequestParam(required = false, defaultValue = "dc.title") String sortBy,
      @RequestParam(required = false, defaultValue = "asc") String sortDir,
      Model model
  ){

    pageSize = Math.min(pageSize, 50); // Limit page size

    // includes now all request parameters, not just "dc.*" ones
    // only keep parameters keys that start with "dc."
    var filteredDcFields = new HashMap<String, List<String>>();
    dcCriteria.forEach((key, values) -> {
      if (key.startsWith("dc.")) {
        filteredDcFields.put(key, values);
      }
    });

    PageRequest pageRequest = buildPageRequest(pageIndex, pageSize, sortBy, sortDir);

    var searchResults = fulltextService.search(
        fulltextQuery,
        filteredDcFields,
        projects,
        pageRequest
    );
    model.addAttribute("searchResults", searchResults.getResults());

    model.addAttribute("dcCriteria", filteredDcFields);
    model.addAttribute("fulltextQuery", fulltextQuery); // ADD THIS LINE

    var projectAbbrs = projectService.findAllProjectAbbrs();
    model.addAttribute("projectAbbrs", projectAbbrs);
    model.addAttribute("selectedProjects", projects);

    // Build current query string for pagination and pass to model
    String currentQuery = buildQueryString(
        fulltextQuery,
        projects,
        MultiValueMap.fromMultiValue(filteredDcFields),
        pageSize
    );
    model.addAttribute("currentQuery", currentQuery);

    return "BaseSearch/fulltext";

  }


  /**
   * Builds PageRequest with validated sort parameters.
   *
   * @param pageIndex Zero-based page index
   * @param pageSize  Number of results per page
   * @param sortBy field to be sorted by
   * @param sortDir sort direction
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
   * Maps and validates user-provided sort field.
   * Allowed fields are defined in allowedSortFields list.
   * @param userField User-provided sort field
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
      log.error(msg);
      throw new FulltextUserQueryException(msg);
    }

    return userField;
  }


  /**
   * Build query string for pagination links.
   * Preserves all current search parameters.
   *
   * @param projects Selected projects
   * @param dcCriteria Dublin Core search criteria
   * @param pageSize Current page size
   * @return URL-encoded query string
   */
  private String buildQueryString(
                                  String fulltextQuery,
                                  Set<String> projects,
                                  MultiValueMap<String, String> dcCriteria,
                                  int pageSize) {

    UriComponentsBuilder builder = UriComponentsBuilder.newInstance();

    // Add projects
    if (projects != null) {
      projects.forEach(project -> builder.queryParam("projects", project));
    }

    builder.queryParam("q", fulltextQuery);
    builder.queryParam("pageSize", pageSize);

    // Add Dublin Core criteria
    dcCriteria.forEach((dcField, values) -> {
      values.forEach(value -> builder.queryParam(dcField, value));
    });

    return builder.build().getQuery();
  }

}
