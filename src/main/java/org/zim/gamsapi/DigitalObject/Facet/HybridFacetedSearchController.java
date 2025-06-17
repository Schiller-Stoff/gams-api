package org.zim.gamsapi.DigitalObject.Facet;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.zim.gamsapi.DigitalObject.DigitalObjectDublinCoreSpecification;
import org.zim.gamsapi.DigitalObject.interfaces.IDigitalObjectService;
import org.zim.gamsapi.System.config.OpenAPIConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * HYBRID FACETED SEARCH CONTROLLER
 *
 * Provides clean REST API for faceted search while using the hybrid approach:
 * Criteria API for main search (your existing logic)
 * Native SQL only for facet counting (performance optimization)
 */
@Slf4j
@RequiredArgsConstructor
@Controller
@RequestMapping({"/api/v1/facets"})
@Tag(name = OpenAPIConfig.FACET_TAG, description = OpenAPIConfig.FACET_TAG_DESCRIPTION)
public class HybridFacetedSearchController {

  private final HybridFacetedSearchService hybridFacetedService;
  private final IDigitalObjectService digitalObjectService;

  // ============================================================================
  // MAIN FACETED SEARCH ENDPOINTS
  // ============================================================================

  /**
   * MAIN ENDPOINT: Complete faceted search with results + facet counts
   * Uses your existing Criteria-based search logic with optimized facet counting
   */
  @GetMapping(path = "/faceted", produces = MimeTypeUtils.APPLICATION_JSON_VALUE)
  @ResponseBody
  @Operation(
      summary = "Hybrid faceted search with optimized performance",
      description = "Complete faceted search implementation using hybrid approach: " +
          "Criteria API for main search (type-safe) + native SQL for facet counts (performance). " +
          "Returns search results with available facet values and their counts.",
      responses = {
          @ApiResponse(responseCode = "200",
              description = "Faceted search results with counts",
              content = @Content(mediaType = MimeTypeUtils.APPLICATION_JSON_VALUE,
                  examples = @ExampleObject(
                      name = "Complete faceted response",
                      description = "Shows both search results and facet counts",
                      value = """
                        {
                          "results": {
                            "content": [
                              {"id": "vipa.123", "objectType": "TEI", "project": {"projectAbbr": "vipa"}},
                              {"id": "vipa.124", "objectType": "TEI", "project": {"projectAbbr": "vipa"}}
                            ],
                            "totalElements": 47,
                            "totalPages": 3,
                            "size": 20,
                            "number": 0
                          },
                          "availableFacets": {
                            "coverage": [
                              {"value": "Wien", "count": 23, "selected": true},
                              {"value": "Nürnberg", "count": 15, "selected": false},
                              {"value": "Berlin", "count": 8, "selected": false}
                            ],
                            "type": [
                              {"value": "Brief", "count": 18, "selected": false},
                              {"value": "Dokument", "count": 12, "selected": false}
                            ],
                            "creator": [
                              {"value": "Schmidt", "count": 5, "selected": false},
                              {"value": "Weber", "count": 3, "selected": false}
                            ]
                          },
                          "selectedFacets": {
                            "coverage": ["Wien"]
                          },
                          "filteredCount": 47,
                          "totalUnfilteredCount": 1250,
                          "metrics": {
                            "searchTimeMs": 45,
                            "facetCountTimeMs": 78,
                            "totalTimeMs": 123,
                            "numberOfFacetFields": 3,
                            "performanceNote": "Good performance - suitable for real-time use"
                          }
                        }
                        """
                  )
              ))
      }
  )
  @Parameter(
      name = "Faceted Search Examples",
      description = "Common usage patterns for faceted search",
      examples = {
          @ExampleObject(
              name = "Basic faceted search",
              value = "?projects=vipa&facetFields=coverage,type,creator",
              description = "Get results with facet counts for specified fields"
          ),
          @ExampleObject(
              name = "Your original use case - FIXED",
              value = "?projects=vipa&coverage=Wien&coverage=Nürnberg&facetFields=coverage,type,creator",
              description = "Multiple coverage values with OR logic + facet counts"
          ),
          @ExampleObject(
              name = "Multi-facet filtering",
              value = "?projects=vipa&coverage=Wien&type=Brief&facetFields=coverage,type,creator",
              description = "Filter by coverage AND type, show remaining facet options"
          ),
          @ExampleObject(
              name = "Auto-discovery",
              value = "?projects=vipa&autoFacets=true",
              description = "Automatically discover and use all available facet fields"
          )
      }
  )
  public FacetedSearchResponse searchFaceted(
      @RequestParam Set<String> projects,
      @RequestParam(required = false) Set<String> facetFields,
      @RequestParam(defaultValue = "false") boolean autoFacets,
      @RequestParam MultiValueMap<String, String> allParams,
      @RequestParam(defaultValue = "0") int pageIndex,
      @RequestParam(defaultValue = "20") int pageSize) {

    pageSize = Math.min(pageSize, 50); // Reasonable limit for faceted search

    // Extract Dublin Core filters from request parameters
    MultiValueMap<String, String> dcFilters = extractDublinCoreFilters(allParams);

    // Auto-discover facet fields if requested
    if (autoFacets || (facetFields == null || facetFields.isEmpty())) {
      facetFields = hybridFacetedService.getAvailableFacetFields(projects);
      log.debug("Auto-discovered facet fields: {}", facetFields);
    }

    log.info("Hybrid faceted search: projects={}, filters={}, facets={}",
        projects, dcFilters, facetFields);

    return hybridFacetedService.searchWithFacets(
        projects, dcFilters, facetFields, PageRequest.of(pageIndex, pageSize));
  }

  /**
   * Simplified endpoint with sensible defaults
   */
  @GetMapping(path = "/faceted/simple", produces = MimeTypeUtils.APPLICATION_JSON_VALUE)
  @ResponseBody
  @Operation(
      summary = "Simplified faceted search with default settings",
      description = "Simplified version of faceted search with commonly used facet fields. " +
          "Perfect for quick implementation of faceted search interfaces.",
      responses = {
          @ApiResponse(responseCode = "200", description = "Simplified faceted search results")
      }
  )
  public FacetedSearchResponse searchFacetedSimple(
      @RequestParam Set<String> projects,
      @RequestParam MultiValueMap<String, String> allParams,
      @RequestParam(defaultValue = "0") int pageIndex,
      @RequestParam(defaultValue = "20") int pageSize) {

    pageSize = Math.min(pageSize, 50);

    // Extract Dublin Core filters
    MultiValueMap<String, String> dcFilters = extractDublinCoreFilters(allParams);

    // Use default facets - customize these based on your domain
    return hybridFacetedService.searchWithDefaultFacets(
        projects, dcFilters, PageRequest.of(pageIndex, pageSize));
  }

  // ============================================================================
  // FACET-SPECIFIC ENDPOINTS
  // ============================================================================

  /**
   * Get facet counts only (without search results)
   * Useful for dynamic UI updates, autocomplete, etc.
   */
  @GetMapping(path = "/facets/counts", produces = MimeTypeUtils.APPLICATION_JSON_VALUE)
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

    MultiValueMap<String, String> dcFilters = extractDublinCoreFilters(allParams);

    return hybridFacetedService.getFacetCountsOnly(projects, dcFilters, facetFields);
  }

  /**
   * Discover available facet fields for projects
   */
  @GetMapping(path = "/facets/available", produces = MimeTypeUtils.APPLICATION_JSON_VALUE)
  @ResponseBody
  @Operation(
      summary = "Discover available facet fields",
      description = "Returns all Dublin Core fields that are available for faceting in the specified projects. " +
          "Useful for building dynamic search interfaces or understanding data structure.",
      responses = {
          @ApiResponse(responseCode = "200", description = "Available facet fields")
      }
  )
  public Set<String> getAvailableFacetFields(@RequestParam Set<String> projects) {
    return hybridFacetedService.getAvailableFacetFields(projects);
  }

  // ============================================================================
  // INTEGRATION WITH YOUR EXISTING ENDPOINTS
  // ============================================================================

  /**
   * Enhanced version of your existing advanced search with facets
   */
  @GetMapping(path = "/dc/advanced-faceted", produces = MimeTypeUtils.APPLICATION_JSON_VALUE)
  @ResponseBody
  @Operation(
      summary = "Enhanced version of your existing advanced search with facets",
      description = "Combines your existing Dublin Core advanced search with faceted counting. " +
          "Drop-in replacement for your current /dc/advanced endpoint with added facet support.",
      responses = {
          @ApiResponse(responseCode = "200", description = "Advanced search with facets")
      }
  )
  public FacetedSearchResponse searchAdvancedWithFacets(
      @RequestParam Set<String> projects,
      @RequestParam(defaultValue = "coverage,type,creator,subject") Set<String> facetFields,
      @RequestParam MultiValueMap<String, String> allParams,
      @RequestParam(defaultValue = "0") int pageIndex,
      @RequestParam(defaultValue = "20") int pageSize) {

    pageSize = Math.min(pageSize, 50);

    MultiValueMap<String, String> dcFilters = extractDublinCoreFilters(allParams);

    log.info("Advanced faceted search (enhanced): projects={}, criteria={}", projects, dcFilters);

    return hybridFacetedService.searchWithFacets(
        projects, dcFilters, facetFields, PageRequest.of(pageIndex, pageSize));
  }

  // ============================================================================
  // HELPER METHODS
  // ============================================================================

  /**
   * Extract Dublin Core filters from request parameters
   * Removes non-DC parameters like pagination, projects, etc.
   */
  private MultiValueMap<String, String> extractDublinCoreFilters(MultiValueMap<String, String> allParams) {
    MultiValueMap<String, String> dcFilters = new LinkedMultiValueMap<>(allParams);

    // Remove non-Dublin Core parameters
    dcFilters.remove("projects");
    dcFilters.remove("facetFields");
    dcFilters.remove("autoFacets");
    dcFilters.remove("pageIndex");
    dcFilters.remove("pageSize");

    return dcFilters;
  }

  // ============================================================================
  // BACKWARD COMPATIBILITY & MIGRATION HELPERS
  // ============================================================================

  /**
   * Migration endpoint: Compare your existing search vs faceted search
   */
  @GetMapping(path = "/migration/compare", produces = MimeTypeUtils.APPLICATION_JSON_VALUE)
  @ResponseBody
  @Operation(
      summary = "Compare existing search vs faceted search",
      description = "Development helper endpoint comparing your existing search results with " +
          "the new faceted search results. Useful for verifying migration and understanding differences.",
      responses = {
          @ApiResponse(responseCode = "200", description = "Migration comparison results")
      }
  )
  public MigrationComparison compareSearchMethods(
      @RequestParam Set<String> projects,
      @RequestParam MultiValueMap<String, String> allParams,
      @RequestParam(defaultValue = "coverage,type,creator") Set<String> facetFields,
      @RequestParam(defaultValue = "0") int pageIndex,
      @RequestParam(defaultValue = "10") int pageSize) {

    pageSize = Math.min(pageSize, 10); // Small size for comparison

    MultiValueMap<String, String> dcFilters = extractDublinCoreFilters(allParams);
    PageRequest pageRequest = PageRequest.of(pageIndex, pageSize);

    // Get results from both approaches
    long startTime = System.currentTimeMillis();

    // Your existing search (Criteria API only)
    long existingStart = System.currentTimeMillis();
    var existingResults = digitalObjectService.searchDigitalObjectsByDublinCoreCriteria(
        dcFilters, projects, DigitalObjectDublinCoreSpecification.SearchMode.EXACT_MATCH, pageRequest);
    long existingTime = System.currentTimeMillis() - existingStart;

    // New faceted search (Hybrid approach)
    long facetedStart = System.currentTimeMillis();
    FacetedSearchResponse facetedResults = hybridFacetedService.searchWithFacets(
        projects, dcFilters, facetFields, pageRequest);
    long facetedTime = System.currentTimeMillis() - facetedStart;

    long totalTime = System.currentTimeMillis() - startTime;

    return MigrationComparison.builder()
        .searchCriteria(dcFilters.toString())
        .existingSearchResults(existingResults.getTotalElements())
        .existingSearchTime(existingTime)
        .facetedSearchResults(facetedResults.getResults().getTotalElements())
        .facetedSearchTime(facetedTime)
        .facetCounts(facetedResults.getAvailableFacets().entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> entry.getValue().size()
            )))
        .resultsMatch(existingResults.getTotalElements() == facetedResults.getResults().getTotalElements())
        .performanceComparison(getPerformanceComparison(existingTime, facetedTime))
        .migrationRecommendations(getMigrationRecommendations(existingTime, facetedTime, facetFields.size()))
        .totalComparisonTime(totalTime)
        .build();
  }

  @Data
  @Builder
  public static class MigrationComparison {
    private String searchCriteria;
    private long existingSearchResults;
    private long existingSearchTime;
    private long facetedSearchResults;
    private long facetedSearchTime;
    private Map<String, Integer> facetCounts;
    private boolean resultsMatch;
    private String performanceComparison;
    private List<String> migrationRecommendations;
    private long totalComparisonTime;
  }

  private String getPerformanceComparison(long existingTime, long facetedTime) {
    if (facetedTime <= existingTime * 1.2) {
      return "Faceted search performance is excellent - minimal overhead for added features";
    } else if (facetedTime <= existingTime * 2) {
      return "Faceted search adds moderate overhead but provides significant value with facet counts";
    } else {
      return "Faceted search has higher latency - consider caching or optimizing facet fields";
    }
  }

  private List<String> getMigrationRecommendations(long existingTime, long facetedTime, int facetFieldCount) {
    List<String> recommendations = new ArrayList<>();

    if (facetedTime > 500) {
      recommendations.add("Consider implementing facet count caching for frequently accessed searches");
    }

    if (facetFieldCount > 5) {
      recommendations.add("Large number of facet fields detected - consider lazy loading or pagination");
    }

    recommendations.add("Hybrid approach successfully preserves your existing search logic while adding facets");
    recommendations.add("Gradual migration recommended: start with key endpoints, then expand");

    if (existingTime < 100 && facetedTime < 200) {
      recommendations.add("Performance is excellent for both approaches - safe to migrate");
    }

    return recommendations;
  }

  // ============================================================================
  // TESTING & DEBUGGING ENDPOINTS
  // ============================================================================

  /**
   * Debug endpoint: Show query execution details
   */
  @GetMapping(path = "/debug/faceted-queries", produces = MimeTypeUtils.APPLICATION_JSON_VALUE)
  @ResponseBody
  @Operation(
      summary = "Debug faceted search query execution",
      description = "Development endpoint showing detailed information about query execution, " +
          "performance metrics, and optimization suggestions.",
      responses = {
          @ApiResponse(responseCode = "200", description = "Debug information")
      }
  )
  public FacetedSearchDebugInfo debugFacetedSearch(
      @RequestParam Set<String> projects,
      @RequestParam(defaultValue = "coverage,type") Set<String> facetFields,
      @RequestParam MultiValueMap<String, String> allParams) {

    MultiValueMap<String, String> dcFilters = extractDublinCoreFilters(allParams);

    // Get available facet fields for comparison
    Set<String> availableFields = hybridFacetedService.getAvailableFacetFields(projects);

    // Execute faceted search with timing
    long start = System.currentTimeMillis();
    FacetedSearchResponse response = hybridFacetedService.searchWithFacets(
        projects, dcFilters, facetFields, PageRequest.of(0, 1)); // Minimal page for debug
    long executionTime = System.currentTimeMillis() - start;

    return FacetedSearchDebugInfo.builder()
        .requestedProjects(projects)
        .requestedFacetFields(facetFields)
        .availableFacetFields(availableFields)
        .dublinCoreFilters(dcFilters)
        .executionTimeMs(executionTime)
        .searchResultCount(response.getResults().getTotalElements())
        .facetFieldsWithCounts(response.getAvailableFacets().entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> entry.getValue().stream().mapToLong(FacetValue::getCount).sum()
            )))
        .performanceMetrics(response.getMetrics())
        .optimizationSuggestions(getOptimizationSuggestions(availableFields, facetFields, executionTime))
        .hybridApproachInfo("Using Criteria API for search + Native SQL for facet counts")
        .build();
  }

  @Data
  @Builder
  public static class FacetedSearchDebugInfo {
    private Set<String> requestedProjects;
    private Set<String> requestedFacetFields;
    private Set<String> availableFacetFields;
    private MultiValueMap<String, String> dublinCoreFilters;
    private long executionTimeMs;
    private long searchResultCount;
    private Map<String, Long> facetFieldsWithCounts;
    private Object performanceMetrics;
    private List<String> optimizationSuggestions;
    private String hybridApproachInfo;
  }

  private List<String> getOptimizationSuggestions(Set<String> availableFields, Set<String> requestedFields, long executionTime) {
    List<String> suggestions = new ArrayList<>();

    if (requestedFields.size() > availableFields.size() / 2) {
      suggestions.add("Consider reducing facet fields for better performance");
    }

    if (executionTime > 1000) {
      suggestions.add("High execution time detected - consider adding database indexes");
      suggestions.add("Consider implementing facet count caching");
    }

    if (requestedFields.size() > 7) {
      suggestions.add("Large number of facet fields - consider lazy loading or user-selectable facets");
    }

    suggestions.add("Current hybrid approach balances type safety with performance");

    return suggestions;
  }

  // ============================================================================
  // PRODUCTION MONITORING ENDPOINTS
  // ============================================================================

  /**
   * Health check for faceted search functionality
   */
  @GetMapping(path = "/faceted/health", produces = MimeTypeUtils.APPLICATION_JSON_VALUE)
  @ResponseBody
  @Operation(
      summary = "Health check for faceted search",
      description = "Monitoring endpoint for checking faceted search system health and performance.",
      responses = {
          @ApiResponse(responseCode = "200", description = "Health check results")
      }
  )
  public FacetedSearchHealthCheck healthCheck(@RequestParam Set<String> projects) {
    long start = System.currentTimeMillis();

    try {
      // Test basic functionality
      Set<String> availableFields = hybridFacetedService.getAvailableFacetFields(projects);

      // Test a simple faceted search
      MultiValueMap<String, String> emptyFilters = new LinkedMultiValueMap<>();
      Set<String> testFields = availableFields.stream().limit(3).collect(Collectors.toSet());

      FacetedSearchResponse testResponse = hybridFacetedService.searchWithFacets(
          projects, emptyFilters, testFields, PageRequest.of(0, 1));

      long responseTime = System.currentTimeMillis() - start;

      return FacetedSearchHealthCheck.builder()
          .status("HEALTHY")
          .responseTimeMs(responseTime)
          .availableFacetFieldCount(availableFields.size())
          .testSearchResultCount(testResponse.getResults().getTotalElements())
          .testFacetFieldCount(testResponse.getAvailableFacets().size())
          .hybridApproachWorking(true)
          .recommendations(responseTime > 500 ?
              List.of("Response time high - consider performance optimization") :
              List.of("System performing well"))
          .timestamp(System.currentTimeMillis())
          .build();

    } catch (Exception e) {
      long responseTime = System.currentTimeMillis() - start;

      return FacetedSearchHealthCheck.builder()
          .status("UNHEALTHY")
          .responseTimeMs(responseTime)
          .error(e.getMessage())
          .hybridApproachWorking(false)
          .recommendations(List.of("Check database connectivity", "Verify Dublin Core data structure"))
          .timestamp(System.currentTimeMillis())
          .build();
    }
  }

  @Data
  @Builder
  public static class FacetedSearchHealthCheck {
    private String status;
    private long responseTimeMs;
    private Integer availableFacetFieldCount;
    private Long testSearchResultCount;
    private Integer testFacetFieldCount;
    private boolean hybridApproachWorking;
    private String error;
    private List<String> recommendations;
    private long timestamp;
  }
}