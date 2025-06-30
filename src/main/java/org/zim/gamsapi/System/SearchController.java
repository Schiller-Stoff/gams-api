package org.zim.gamsapi.System;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.util.UriComponentsBuilder;
import org.zim.gamsapi.DigitalObject.DigitalObjectDublinCoreSpecification;
import org.zim.gamsapi.DigitalObject.dto.DigitalObjectSearchResultDTO;
import org.zim.gamsapi.DigitalObject.interfaces.DigitalObjectListItemView;
import org.zim.gamsapi.DigitalObject.interfaces.IDigitalObjectService;
import org.zim.gamsapi.Project.interfaces.IProjectService;
import org.zim.gamsapi.System.config.OpenAPIConfig;
import org.zim.gamsapi.System.dto.PagedResponse;

import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Controller for searching digital objects.
 */
@Slf4j
@RequiredArgsConstructor
@Controller
@RequestMapping({"/api/v1/search" })
@Tag(name = OpenAPIConfig.SEARCH_TAG, description = OpenAPIConfig.SEARCH_TAG_DESCRIPTION)
public class SearchController {
  /**
   * Service for searching digital objects.
   */
  private final IDigitalObjectService digitalObjectService;
  private final IProjectService projectService;

  /**
   * Fulltext search over all dublin core fields of a digital object.
   * @param projects list of project abbreviations
   * @param dcFields list of DublinCoreElement names
   * @param search fulltext search string
   * @param pageIndex page index
   * @param pageSize page size
   * @return a page of digital objects
   */
  @GetMapping(path = "/dc/fulltext", produces = {
      MimeTypeUtils.APPLICATION_JSON_VALUE,
      MimeTypeUtils.APPLICATION_XML_VALUE
  })
  @ResponseBody
  @Operation(
      summary = "Dublin core fulltext search based on digital objects and multiple projects.",
      description = "Searches for digital objects based on a fulltext search over all Dublin Core fields. The search is performed on multiple projects and can include multiple Dublin Core fields.",
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
  public PagedResponse<DigitalObjectListItemView> searchDigitalObjectsViaDublinCoreFulltext(
      @RequestParam Set<String> projects,
      // dublin core search parameters
      @RequestParam(
          required = false,
          // sets default value empty set
          defaultValue = ""
      ) Set<String> dcFields,
      @RequestParam String search,
      // for pagination
      @RequestParam(defaultValue = "0") int pageIndex,
      @RequestParam(defaultValue = "20") int pageSize
  ){

    // limit page size
    if (pageSize >= 100) {
      pageSize = 100;
    }

    //TODO include dublin core data into response?

    return digitalObjectService.searchByDCFulltext(
        projects,
        dcFields,
        search,
        PageRequest.of(pageIndex, pageSize)
    );
  }

  @GetMapping(path = "/dc", produces = {
      MimeTypeUtils.APPLICATION_JSON_VALUE,
      MimeTypeUtils.APPLICATION_XML_VALUE
  })
  @ResponseBody
  @Operation(
      summary = "Advanced multi-criteria Dublin Core search for digital objects",
      description = "Advanced search supporting multiple Dublin Core criteria with different search modes. " +
          "Supports exact match, contains, and fulltext search modes.",
      responses = {
          @ApiResponse(responseCode = "200", description = "Digital objects found"),
          @ApiResponse(responseCode = "400", description = "Invalid request parameters")
      }
  )
  @Parameter(
      name = "dcEntries",
      required = false,
      examples = {
          @ExampleObject(
              name = "dc.type search",
              summary = "Return all dc.type fields with value 'Brief'",
              value = "?projects=vipa&dc.type=Brief",
              description = "Search for type field entries"
          ),
          @ExampleObject(
              name = "Multi-field search",
              summary = "Search across multiple DC fields",
              value = "?projects=vipa&dc.title=Vienna&dc.creator=John&dc.subject=History",
              description = "Combined search across multiple fields"
          )
      },
      description = "Multi-value map of Dublin Core entries to filter by.",
      schema = @Schema(type = "object")
  )
  @Parameter(
      name = "format",
      description = "Format of the response. Defaults to JSON.",
      required = false,
      schema = @Schema(type = "string", defaultValue = "json", examples = "xml,json")
  )
  public PagedResponse<DigitalObjectSearchResultDTO> searchDigitalObjectsByDublinCoreAdvanced(
      @RequestParam MultiValueMap<String, String> dcCriteria,
      @RequestParam Set<String> projects,
      @RequestParam(defaultValue = "EXACT_MATCH") DigitalObjectDublinCoreSpecification.SearchMode searchMode,
      @RequestParam(defaultValue = "0") int pageIndex,
      @RequestParam(defaultValue = "20") int pageSize) {

    pageSize = Math.min(pageSize, 20); // Limit page size

    // includes now all request parameters, not just "dc.*" ones
    // only keep parameters keys that start with "dc."
    var filteredDcFields = new HashMap<String, List<String>>();
    dcCriteria.forEach((key, values) -> {
      if (key.startsWith("dc.")) {
        String newKey = key.substring(3); // Remove "dc." prefix
        filteredDcFields.put(newKey, values);
      }
    });

    log.debug("Advanced DC search - criteria: {}, projects: {}, mode: {}",
        dcCriteria, projects, searchMode);

    return digitalObjectService.searchDigitalObjectsByDublinCoreCriteria(
        MultiValueMap.fromMultiValue(filteredDcFields), projects, searchMode, PageRequest.of(pageIndex, pageSize));
  }

  /**
   * Display the Dublin Core search form and results.
   *
   * @param projects Selected project abbreviations
   * @param searchMode Search mode (EXACT_MATCH, CONTAINS, FULLTEXT)
   * @param allParams All request parameters (including dynamic DC criteria)
   * @param pageIndex Current page index
   * @param pageSize Number of results per page
   * @param model Spring MVC model
   * @return Thymeleaf template name
   */
  @GetMapping("/dc/view")
  public String searchView(
      @RequestParam(required = false) Set<String> projects,
      @RequestParam(defaultValue = "EXACT_MATCH") DigitalObjectDublinCoreSpecification.SearchMode searchMode,
      @RequestParam MultiValueMap<String, String> allParams,
      @RequestParam(defaultValue = "0") int pageIndex,
      @RequestParam(defaultValue = "20") int pageSize,
      Model model) {

    log.debug("DC Search View - projects: {}, searchMode: {}, pageIndex: {}, pageSize: {}",
        projects, searchMode, pageIndex, pageSize);

    // Add available projects to model for form population
    model.addAttribute("projects", projectService.findAll());
    model.addAttribute("selectedProjects", projects);
    model.addAttribute("searchMode", searchMode);
    model.addAttribute("pageSize", pageSize);

    // Extract Dublin Core criteria from request parameters
    MultiValueMap<String, String> dcCriteria = extractDublinCoreCriteria(allParams);
    model.addAttribute("dcCriteria", dcCriteria);

    // Perform search if projects are selected and DC criteria exist
    if (projects != null && !projects.isEmpty() && !dcCriteria.isEmpty()) {
      try {
        // Limit page size to prevent excessive load
        pageSize = Math.min(pageSize, 100);

        PagedResponse<DigitalObjectSearchResultDTO> searchResults = digitalObjectService
            .searchDigitalObjectsByDublinCoreCriteria(
                dcCriteria,
                projects,
                searchMode,
                PageRequest.of(pageIndex, pageSize)
            );

        model.addAttribute("searchResults", searchResults);

        // Build current query string for pagination
        String currentQuery = buildQueryString(projects, searchMode, dcCriteria, pageSize);
        model.addAttribute("currentQuery", currentQuery);

        log.debug("Search completed - found {} results", searchResults.getPagination().getTotalElements());

      } catch (Exception e) {
        log.error("Error performing Dublin Core search", e);
        model.addAttribute("searchError", "An error occurred while searching. Please try again.");
      }
    } else if (projects != null && !projects.isEmpty() && dcCriteria.isEmpty()) {
      model.addAttribute("searchInfo", "Please add at least one Dublin Core search criterion.");
    }

    return "search/dublin-core-search";
  }

  /**
   * Extract Dublin Core search criteria from all request parameters.
   * Filters out system parameters and organizes DC field-value pairs.
   *
   * @param allParams All request parameters
   * @return Cleaned Dublin Core criteria
   */
  private MultiValueMap<String, String> extractDublinCoreCriteria(MultiValueMap<String, String> allParams) {
    MultiValueMap<String, String> dcCriteria = new LinkedMultiValueMap<>();

    // System parameters to exclude from DC criteria
    Set<String> systemParams = Set.of(
        "projects", "searchMode", "pageIndex", "pageSize",
        "dcField", "dcValue" // form helper fields
    );

    // Standard Dublin Core elements we support
    Set<String> supportedDcFields = Set.of(
        "title", "creator", "subject", "description", "publisher",
        "contributor", "date", "type", "format", "identifier",
        "source", "language", "relation", "coverage", "rights"
    );

    allParams.forEach((key, values) -> {
      if (!systemParams.contains(key) && supportedDcFields.contains(key)) {
        // Filter out empty values
        List<String> nonEmptyValues = values.stream()
            .filter(value -> value != null && !value.trim().isEmpty())
            .collect(Collectors.toList());

        if (!nonEmptyValues.isEmpty()) {
          dcCriteria.put(key, nonEmptyValues);
        }
      }
    });

    log.debug("Extracted DC criteria: {}", dcCriteria);
    return dcCriteria;
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

  /**
   * Redirect endpoint for backward compatibility.
   * Redirects old search URLs to the new view endpoint.
   */
  @GetMapping
  public String redirectToView() {
    return "redirect:/api/v1/search/dc/view";
  }

}
