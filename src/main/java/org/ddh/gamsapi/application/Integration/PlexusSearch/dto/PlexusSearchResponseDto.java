package org.ddh.gamsapi.application.Integration.PlexusSearch.dto;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Response DTO for Plexus Search queries.
 *
 * Contains search results, pagination info, facets, and performance hints.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Response from Plexus Search query")
@Slf4j
public class PlexusSearchResponseDto {

  @Schema(description = "Matching documents", required = true)
  private List<Map<String, Object>> documents;

  @Schema(description = "Total number of matching documents", example = "45678", required = true)
  private Long totalCount;

  @Schema(description = "Starting offset for this page", example = "0", required = true)
  private Integer start;

  @Schema(description = "Number of documents in this response", example = "20", required = true)
  private Integer rows;

  @Schema(description = "Whether there are more results available", example = "true")
  private Boolean hasMore;

  @Schema(description = "Query execution time in milliseconds", example = "234")
  private Long executionTimeMs;

  @Schema(description = "Highlighting information (if requested)")
  private Map<String, Map<String, List<String>>> highlighting;

  @Schema(description = "Facet information (if requested)")
  private Map<String, Map<String, Long>> facets;

  @Schema(description = "Next cursor mark for cursor-based pagination")
  private String nextCursorMark;

  @Schema(description = "Debug information (if requested)")
  private Map<String, Object> debugInfo;

  @Schema(description = "Query optimization hints and warnings")
  private List<String> hints;

  @Schema(description = "Maximum score in results")
  private Float maxScore;

  /**
   * Parse Solr JSON response into PlexusSearchResponse.
   * TODO tests missing
   * @param solrJsonResponse Raw JSON response from Solr
   * @param originalRequest Original query request
   * @return Parsed PlexusSearchResponse
   */
  public static PlexusSearchResponseDto from(String solrJsonResponse, PlexusSearchQueryRequestDto originalRequest) {
    try {
      ObjectMapper mapper = new ObjectMapper();
      JsonNode root = mapper.readTree(solrJsonResponse);
      JsonNode responseNode = root.path("response");

      // Extract documents
      List<Map<String, Object>> docs = new ArrayList<>();
      if (responseNode.has("docs")) {
        for (JsonNode doc : responseNode.get("docs")) {
          docs.add(mapper.convertValue(doc, Map.class));
        }
      }

      // Extract total count
      long numFound = responseNode.path("numFound").asLong(0);

      // Extract highlighting
      Map<String, Map<String, List<String>>> highlighting = null;
      if (root.has("highlighting")) {
        highlighting = mapper.convertValue(root.get("highlighting"), Map.class);
      }

      // Extract facets
      Map<String, Map<String, Long>> facets = null;
      if (root.has("facet_counts") && root.path("facet_counts").has("facet_fields")) {
        facets = parseFacets(root.path("facet_counts").path("facet_fields"));
      }

      // Extract cursor mark for pagination
      String nextCursorMark = root.path("nextCursorMark").asText(null);

      // Extract debug info
      Map<String, Object> debugInfo = null;
      if (root.has("debug")) {
        debugInfo = mapper.convertValue(root.get("debug"), Map.class);
      }

      // Extract query time
      Long qTime = root.path("responseHeader").path("QTime").asLong(0);

      // Extract max score
      Float maxScore = responseNode.path("maxScore").floatValue();

      // Generate optimization hints
      List<String> hints = generateHints(originalRequest, numFound, qTime);

      return PlexusSearchResponseDto.builder()
          .documents(docs)
          .totalCount(numFound)
          .start(responseNode.path("start").asInt(0))
          .rows(docs.size())
          .hasMore(responseNode.path("start").asInt(0) + docs.size() < numFound)
          .executionTimeMs(qTime)
          .highlighting(highlighting)
          .facets(facets)
          .nextCursorMark(nextCursorMark)
          .debugInfo(debugInfo)
          .hints(hints)
          .maxScore(maxScore)
          .build();

    } catch (Exception e) {
      log.error("Failed to parse Solr response: {}", e.getMessage(), e);
      throw new RuntimeException("Failed to parse Solr response", e);
    }
  }

  /**
   * Parse Solr facet response format into a more usable structure.
   */
  private static Map<String, Map<String, Long>> parseFacets(JsonNode facetFields) {
    // Solr returns facets as: {"field": ["value1", count1, "value2", count2, ...]}
    // We convert to: {"field": {"value1": count1, "value2": count2, ...}}
    Map<String, Map<String, Long>> result = new java.util.HashMap<>();

    facetFields.fields().forEachRemaining(entry -> {
      String fieldName = entry.getKey();
      JsonNode values = entry.getValue();
      Map<String, Long> facetMap = new java.util.HashMap<>();

      for (int i = 0; i < values.size(); i += 2) {
        String value = values.get(i).asText();
        Long count = values.get(i + 1).asLong();
        facetMap.put(value, count);
      }

      result.put(fieldName, facetMap);
    });

    return result;
  }

  /**
   * Generate query optimization hints based on query characteristics and results.
   */
  private static List<String> generateHints(PlexusSearchQueryRequestDto request, long totalCount, long qTime) {
    List<String> hints = new ArrayList<>();

    // Slow query hint
    if (qTime > 1000) {
      hints.add("Query took over 1 second. Consider adding more specific filter queries.");
    }

    // Large result set hint
    if (totalCount > 100000) {
      hints.add("Query matched " + totalCount + " documents. Consider narrowing your search with filter queries.");
    }

    // Wildcard query hint
    if (request.getQuery() != null && request.getQuery().contains("*")) {
      if (request.getQuery().startsWith("*") || request.getQuery().contains(":*")) {
        hints.add("Leading wildcards are expensive. Consider restructuring your query.");
      }
    }

    // Deep pagination hint
    if (request.getStart() != null && request.getStart() > 10000) {
      hints.add("Deep pagination detected. Use cursor-based pagination (cursorMark) for better performance.");
    }

    // Large row count hint
    if (request.getRows() != null && request.getRows() > 100) {
      hints.add("Fetching " + request.getRows() + " rows may impact performance. Consider pagination with smaller page sizes.");
    }

    // Missing filter queries hint
    if ((request.getFilterQueries() == null || request.getFilterQueries().isEmpty()) && totalCount > 10000) {
      hints.add("No filter queries specified. Adding filter queries (fq) can significantly improve performance.");
    }

    // Excessive faceting hint
    if (request.getFacetFields() != null && request.getFacetFields().size() > 5) {
      hints.add("Faceting on " + request.getFacetFields().size() + " fields may be expensive. Consider reducing facet fields.");
    }

    return hints.isEmpty() ? null : hints;
  }
}
