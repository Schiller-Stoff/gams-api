package org.ddh.gamsapi.application.Integration.BaseSearch.Fulltext;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ddh.gamsapi.application.Integration.BaseSearch.BaseSearchProperties;
import org.ddh.gamsapi.application.Integration.Common.exceptions.IntegrationServiceException;
import org.ddh.gamsapi.domain.DigitalObject.DigitalObjectDublinCoreSpecification;
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
import org.springframework.web.bind.annotation.RestController;
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
@Tag(name = OpenAPIConfig.INTEGRATION_TAG, description = OpenAPIConfig.INTEGRATION_TAG_DESCRIPTION)
public class FulltextController {

  public static final String FULLTEXT_SEARCH_PATH = "/api/v1/integration/gsearch/fulltext";

  private final FulltextService fulltextService;

  private final IProjectService projectService;


  /**
   * TODO jdoc
   * TODO OpenAPI doc
   * @param dcCriteria
   * @param projects
   * @param fulltextQuery
   * @param pageIndex
   * @param pageSize
   * @param sortBy
   * @param sortDir
   * @return
   */
  @GetMapping(path = FULLTEXT_SEARCH_PATH, produces = {
      MimeTypeUtils.APPLICATION_JSON_VALUE,
      MimeTypeUtils.APPLICATION_XML_VALUE
  })
  @ResponseBody
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

    // TODO rename method
    return fulltextService.searchDigitalObjectsByDublinCoreCriteria(
        fulltextQuery,
        filteredDcFields,
        projects,
        pageRequest
    );

  }

  /**
   * TODO jdoc
   * TODO OpenAPI doc
   *
   * @param dcCriteria
   * @param projects
   * @param fulltextQuery
   * @param pageIndex
   * @param pageSize
   * @param sortBy
   * @param sortDir
   * @return
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

    var searchResults = fulltextService.searchDigitalObjectsByDublinCoreCriteria(
        fulltextQuery,
        filteredDcFields,
        projects,
        pageRequest
    );
    model.addAttribute("searchResults", searchResults.getResults());

    model.addAttribute("dcCriteria", filteredDcFields);
    model.addAttribute("fulltextQuery", fulltextQuery); // ADD THIS LINE

    // TODO query all available projects and add to model / view
    // (only need projectAbbr + description - maybe create a lightweight DTO for that?)
    var projectAbbrs = projectService.findAllProjectAbbrs();
    model.addAttribute("projectAbbrs", projectAbbrs);
    model.addAttribute("selectedProjects", projects);

    // Build current query string for pagination
    // TODO is this necessary?
    String currentQuery = buildQueryString(
        projects,
        // TODO remove the following hardcoding
        DigitalObjectDublinCoreSpecification.SearchMode.FULLTEXT,
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
   * @param sortBy    TODO
   * @param sortDir   TODO
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
   * Build query string for pagination links.
   * Preserves all current search parameters.
   *
   * @param projects Selected projects
   * @param searchMode Current search mode
   * @param dcCriteria Dublin Core search criteria
   * @param pageSize Current page size
   * @return URL-encoded query string
   */
  private String buildQueryString(Set<String> projects,
                                  DigitalObjectDublinCoreSpecification.SearchMode searchMode,
                                  MultiValueMap<String, String> dcCriteria,
                                  int pageSize) {

    UriComponentsBuilder builder = UriComponentsBuilder.newInstance();

    // Add projects
    if (projects != null) {
      projects.forEach(project -> builder.queryParam("projects", project));
    }

    // Add search mode
    builder.queryParam("searchMode", searchMode.name());
    builder.queryParam("pageSize", pageSize);

    // Add Dublin Core criteria
    dcCriteria.forEach((dcField, values) -> {
      values.forEach(value -> builder.queryParam(dcField, value));
    });

    return builder.build().getQuery();
  }

}
