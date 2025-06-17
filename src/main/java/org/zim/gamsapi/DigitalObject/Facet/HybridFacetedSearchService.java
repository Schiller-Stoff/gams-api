package org.zim.gamsapi.DigitalObject.Facet;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.zim.gamsapi.DigitalObject.DigitalObjectDublinCoreSpecification;
import org.zim.gamsapi.DigitalObject.interfaces.IDigitalObjectService;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * HYBRID SERVICE: Combines Criteria API search with native SQL facet counting
 */
@RequiredArgsConstructor
@Slf4j
@Service
public class HybridFacetedSearchService {

  private final IDigitalObjectService digitalObjectService; // Your existing service
  private final FacetCountRepository facetCountRepository;  // New native SQL repository

  /**
   * MAIN HYBRID FACETED SEARCH METHOD
   *
   * Uses:
   * ✅ Your existing Criteria-based search for main results
   * ✅ Native SQL only for facet counting performance
   */
  public FacetedSearchResponse searchWithFacets(
      Set<String> projectAbbrs,
      MultiValueMap<String, String> selectedFacets,
      Set<String> facetFields,
      Pageable pageable) {

    long startTime = System.currentTimeMillis();

    log.debug("Hybrid faceted search: projects={}, filters={}, facetFields={}",
        projectAbbrs, selectedFacets, facetFields);

    // STEP 1: Main search using your existing Criteria-based service
    // This keeps all your existing logic, specifications, and type safety
    long searchStart = System.currentTimeMillis();
    var searchResults = digitalObjectService.searchDigitalObjectsByDublinCoreCriteria(
        selectedFacets,
        projectAbbrs,
        DigitalObjectDublinCoreSpecification.SearchMode.EXACT_MATCH,
        pageable
    );
    long searchTime = System.currentTimeMillis() - searchStart;

    // STEP 2: Facet counting using optimized native SQL
    // This is the ONLY place we use native SQL for performance
    long facetStart = System.currentTimeMillis();
    Map<String, List<FacetValue>> availableFacets = facetCountRepository.getFacetCounts(
        projectAbbrs, selectedFacets, facetFields);
    long facetTime = System.currentTimeMillis() - facetStart;

    // STEP 3: Get baseline total (optional, cached in real implementation)
    long totalUnfilteredCount = facetCountRepository.getTotalObjectCount(projectAbbrs);

    long totalTime = System.currentTimeMillis() - startTime;

    // Build response
    return FacetedSearchResponse.builder()
        .results(searchResults)
        .availableFacets(availableFacets)
        .selectedFacets(selectedFacets)
        .filteredCount(searchResults.getTotalElements())
        .totalUnfilteredCount(totalUnfilteredCount)
        .metrics(FacetSearchMetrics.builder()
            .searchTimeMs(searchTime)
            .facetCountTimeMs(facetTime)
            .totalTimeMs(totalTime)
            .numberOfFacetFields(facetFields.size())
            .performanceNote(getPerformanceNote(searchTime, facetTime, facetFields.size()))
            .build())
        .build();
  }

  /**
   * Auto-discovery: Get available facet fields for a project
   */
  public Set<String> getAvailableFacetFields(Set<String> projectAbbrs) {
    return facetCountRepository.getAvailableFacetFields(projectAbbrs);
  }

  /**
   * Simplified search with default facet fields
   */
  public FacetedSearchResponse searchWithDefaultFacets(
      Set<String> projectAbbrs,
      MultiValueMap<String, String> selectedFacets,
      Pageable pageable) {

    // Default facet fields - customize based on your domain
    Set<String> defaultFacets = Set.of("coverage", "type", "creator", "subject", "language");

    return searchWithFacets(projectAbbrs, selectedFacets, defaultFacets, pageable);
  }

  /**
   * Get facet counts only (without main search results)
   * Useful for dynamic UI updates
   */
  public Map<String, List<FacetValue>> getFacetCountsOnly(
      Set<String> projectAbbrs,
      MultiValueMap<String, String> selectedFacets,
      Set<String> facetFields) {

    return facetCountRepository.getFacetCounts(projectAbbrs, selectedFacets, facetFields);
  }

  private String getPerformanceNote(long searchTime, long facetTime, int facetFieldCount) {
    long totalTime = searchTime + facetTime;

    if (totalTime < 100) {
      return "Excellent performance - hybrid approach working well";
    } else if (totalTime < 500) {
      return "Good performance - suitable for real-time use";
    } else if (facetTime > searchTime * 2) {
      return "Facet counting is bottleneck - consider caching for frequently accessed facets";
    } else {
      return "Consider optimizing database indexes or reducing facet field count";
    }
  }
}
