package org.ddh.gamsapi.application.Integration.PlexusSearch.dto;


import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Request DTO for Plexus Search queries.
 *
 * Allows projects to specify custom Solr queries with validation and safety limits.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Custom Solr query request for Plexus Search")
public class PlexusSearchQueryRequestDto {

  @NotBlank(message = "Query string is required")
  @Size(max = 1000, message = "Query string cannot exceed 1000 characters")
  @Schema(
      description = "Solr query string (q parameter)",
      example = "word:Tagsatzung AND lemma:Tagsatzung"
  )
  private String query;

  @Min(value = 0, message = "Start must be non-negative")
  @Max(value = 100000, message = "Start cannot exceed 100,000 (use cursor pagination for deep paging)")
  @Schema(description = "Starting offset for pagination", example = "0", defaultValue = "0")
  @Builder.Default
  private Integer start = 0;

  @Min(value = 1, message = "Rows must be at least 1")
  @Max(value = 1000, message = "Cannot fetch more than 1000 rows per request")
  @Schema(description = "Number of results to return", example = "20", defaultValue = "20")
  @Builder.Default
  private Integer rows = 20;

  @Size(max = 200, message = "Sort string cannot exceed 200 characters")
  @Pattern(
      regexp = "^[a-zA-Z0-9_]+ (asc|desc)(,[a-zA-Z0-9_]+ (asc|desc))*$|^$",
      message = "Sort format must be 'field asc' or 'field desc', comma-separated"
  )
  @Schema(description = "Sort order", example = "page_number asc, score desc")
  @Builder.Default
  private String sort = "id desc";

  @Size(max = 20, message = "Cannot specify more than 20 filter queries")
  @Schema(description = "Filter queries (fq parameters) for faceted search", example = "[\"corpus_id:medieval_01\", \"language:de\"]")
  @Builder.Default
  private List<String> filterQueries = new ArrayList<>();

  @Schema(description = "Enable result highlighting", example = "true", defaultValue = "false")
  @Builder.Default
  private Boolean highlight = false;

  @Size(max = 10, message = "Cannot highlight more than 10 fields")
  @Schema(description = "Fields to highlight", example = "[\"word\", \"context\"]")
  @Builder.Default
  private List<String> highlightFields = new ArrayList<>();

  @Min(value = 50, message = "Highlight snippet size must be at least 50")
  @Max(value = 500, message = "Highlight snippet size cannot exceed 500")
  @Schema(description = "Highlight fragment size", example = "200", defaultValue = "200")
  @Builder.Default
  private Integer highlightSnippetSize = 200;

  @Size(max = 20, message = "Cannot facet on more than 20 fields")
  @Schema(description = "Fields to facet on", example = "[\"corpus_id\", \"language\"]")
  @Builder.Default
  private List<String> facetFields = new ArrayList<>();

  @Min(value = 1, message = "Facet limit must be at least 1")
  @Max(value = 100, message = "Facet limit cannot exceed 100")
  @Schema(description = "Maximum facet values to return per field", example = "10", defaultValue = "10")
  @Builder.Default
  private Integer facetLimit = 10;

  @Min(value = 0, message = "Facet min count must be non-negative")
  @Schema(description = "Minimum count for facet values", example = "1", defaultValue = "1")
  @Builder.Default
  private Integer facetMinCount = 1;

  @Schema(description = "Enable query debugging", example = "false", defaultValue = "false")
  @Builder.Default
  private Boolean debug = false;

  @Size(max = 50, message = "Cannot specify more than 50 fields")
  @Schema(description = "Fields to return (leave empty for all stored fields)", example = "[\"id\", \"word\", \"context\"]")
  @Builder.Default
  private List<String> fields = new ArrayList<>();

  @Schema(description = "Cursor mark for cursor-based pagination (more efficient for deep paging)", example = "*")
  private String cursorMark;

  @Schema(description = "Additional custom parameters (advanced use only)")
  @Builder.Default
  private Map<String, String> customParams = new HashMap<>();
}
