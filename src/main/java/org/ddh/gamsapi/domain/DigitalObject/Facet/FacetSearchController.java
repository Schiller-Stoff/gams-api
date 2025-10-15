package org.ddh.gamsapi.domain.DigitalObject.Facet;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.ddh.gamsapi.infrastructure.System.config.OpenAPIConfig;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * HYBRID FACETED SEARCH CONTROLLER
 * Provides clean REST API for faceted search while using the hybrid approach:
 * Criteria API for main search (your existing logic)
 * Native SQL only for facet counting (performance optimization)
 */
@Slf4j
@RequiredArgsConstructor
@Controller
@RequestMapping({"/api/v1/facets"})
@Tag(name = OpenAPIConfig.FACET_TAG, description = OpenAPIConfig.FACET_TAG_DESCRIPTION)
public class FacetSearchController {

  private final FacetSearchService hybridFacetedService;

  /**
   * Simplified endpoint with sensible defaults
   */
  @GetMapping( produces = {
      MimeTypeUtils.APPLICATION_JSON_VALUE,
      MimeTypeUtils.APPLICATION_XML_VALUE
  })
  @ResponseBody
  @Operation(
      summary = "Simplified faceted search with default settings",
      description = "Simplified version of faceted search with commonly used facet fields. " +
          "Perfect for quick implementation of faceted search interfaces.",
      responses = {
          @ApiResponse(responseCode = "200", description = "Simplified faceted search results")
      }
  )
  @Parameter(
      name = "Faceted Search Examples",
      description = "Common usage patterns for faceted search",
      examples = {
          @ExampleObject(
              name = "Basic saerch for project",
              value = "?projects=vipa",
              description = "Get results with facet counts for specified fields"
          ),
          @ExampleObject(
              name = "More complex use case",
              value = "?projects=vipa&coverage=Wien&dc.coverage=Nürnberg",
              description = "Multiple coverage values with OR logic + facet counts"
          ),
          @ExampleObject(
              name = "Multi-facet filtering",
              value = "?projects=vipa&dc.coverage=Wien&dc.type=Brief",
              description = "Filter by coverage AND type, show remaining facet options"
          )
      }
  )
  public FacetSearchResponse searchFacetedSimple(
      @RequestParam Set<String> projects,
      @RequestParam MultiValueMap<String, String> dcFilters,
      @RequestParam(defaultValue = "0") int pageIndex,
      @RequestParam(defaultValue = "20") int pageSize) {

    pageSize = Math.min(pageSize, 50);

    // Extract Dublin Core filters
    MultiValueMap<String, String> dcFiltersCleaned = extractDublinCoreFilters(dcFilters);

    // Use default facets - customize these based on your domain
    return hybridFacetedService.searchWithDefaultFacets(
        projects, dcFiltersCleaned, PageRequest.of(pageIndex, pageSize, Sort.by("id").ascending()));
  }

  /**
   * Get facet counts only (without search results)
   * Useful for dynamic UI updates, autocomplete, etc.
   */
  @GetMapping(path = "/counts", produces = {
      MimeTypeUtils.APPLICATION_JSON_VALUE,
      MimeTypeUtils.APPLICATION_XML_VALUE
  })
  @ResponseBody
  @Operation(
      summary = "Get facet counts without search results",
      description = "Returns only facet counts for specified fields, without executing the main search. " +
          "Useful for dynamic UI updates, autocomplete widgets, or when you only need facet information.",
      responses = {
          @ApiResponse(responseCode = "200", description = "Facet counts only")
      }
  )
  public Map<String, List<FacetValue>> getFacetCounts(
      @RequestParam Set<String> projects,
      @RequestParam Set<String> facetFields,
      @RequestParam MultiValueMap<String, String> allParams) {

    // TODO should only allow my domain's facet fields (coverage, type, creator, subject, format, language, etc.)

    MultiValueMap<String, String> dcFilters = extractDublinCoreFilters(allParams);
    return hybridFacetedService.getFacetCountsOnly(projects, dcFilters, facetFields);
  }

  /**
   * Extract Dublin Core filters from request parameters
   * Removes non-DC parameters like pagination, projects, etc.
   */
  private MultiValueMap<String, String> extractDublinCoreFilters(MultiValueMap<String, String> allParams) {

    // includes now all request parameters, not just "dc.*" ones
    // only keep parameters keys that start with "dc."
    var filteredDcFields = new HashMap<String, List<String>>();
    allParams.forEach((key, values) -> {
      if (key.startsWith("dc.")) {
        String newKey = key.substring(3); // Remove "dc." prefix
        filteredDcFields.put(newKey, values);
      }
    });

    return MultiValueMap.fromMultiValue(filteredDcFields);
  }

}