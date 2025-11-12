package org.ddh.gamsapi.application.Integration.PlexusSearch.validation;


import lombok.extern.slf4j.Slf4j;
import org.ddh.gamsapi.application.Integration.PlexusSearch.dto.PlexusSearchQueryRequestDto;
import org.ddh.gamsapi.application.Integration.PlexusSearch.exceptions.PlexusSearchForbiddenQueryException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Validates Plexus Search queries for security and complexity.
 *
 * CRITICAL SECURITY COMPONENT:
 * - Prevents expensive queries that could degrade Solr performance
 * - Blocks admin operations and schema modifications
 * - Enforces resource limits to prevent abuse
 *
 * @author GAMS-API Team
 */
@Component
@Slf4j
public class PlexusSearchQueryValidator {

  // Dangerous patterns that could be admin operations or exploits
  private static final List<Pattern> FORBIDDEN_PATTERNS = List.of(
      Pattern.compile("\\bdelete\\b", Pattern.CASE_INSENSITIVE),
      Pattern.compile("\\bupdate\\b", Pattern.CASE_INSENSITIVE),
      Pattern.compile("\\bcommit\\b", Pattern.CASE_INSENSITIVE),
      Pattern.compile("\\brollback\\b", Pattern.CASE_INSENSITIVE),
      Pattern.compile("\\boptimize\\b", Pattern.CASE_INSENSITIVE),
      Pattern.compile("/admin", Pattern.CASE_INSENSITIVE),
      Pattern.compile("/schema", Pattern.CASE_INSENSITIVE),
      Pattern.compile("/config", Pattern.CASE_INSENSITIVE),
      Pattern.compile("/cores", Pattern.CASE_INSENSITIVE),
      Pattern.compile("<script", Pattern.CASE_INSENSITIVE), // XSS attempt
      Pattern.compile("javascript:", Pattern.CASE_INSENSITIVE) // XSS attempt
  );

  // Maximum complexity limits
  private static final int MAX_BOOLEAN_CLAUSES = 100;
  private static final int MAX_FILTER_QUERIES = 20;
  private static final int MAX_FACET_FIELDS = 20;
  private static final int MAX_ROWS_PER_QUERY = 1000;
  private static final int MAX_DEEP_PAGINATION_OFFSET = 100000;
  private static final int MAX_QUERY_LENGTH = 1000;

  /**
   * Validates a query request against all security and complexity rules.
   *
   * @param request The query request to validate
   * @param projectAbbr The project making the request (for logging)
   * @throws PlexusSearchForbiddenQueryException if validation fails
   */
  public void validateQuery(PlexusSearchQueryRequestDto request, String projectAbbr) {
    log.debug("Validating Plexus query for project: {}", projectAbbr);

    List<String> violations = new ArrayList<>();

    // 1. Check for forbidden patterns in query string
    validateQueryString(request.getQuery(), violations);

    // 2. Validate pagination parameters
    validatePagination(request, violations);

    // 3. Validate filter queries
    validateFilterQueries(request.getFilterQueries(), violations);

    // 4. Validate faceting parameters
    validateFaceting(request, violations);

    // 5. Validate query complexity
    validateComplexity(request, violations);

    // 6. Validate sort parameters
    validateSort(request.getSort(), violations);

    // 7. Validate custom parameters
    validateCustomParams(request.getCustomParams(), violations);

    // If any violations found, reject the query
    if (!violations.isEmpty()) {
      String errorMsg = String.format(
          "Query validation failed for project '%s': %s",
          projectAbbr,
          String.join("; ", violations)
      );
      log.warn(errorMsg);
      throw new PlexusSearchForbiddenQueryException(errorMsg);
    }

    log.debug("Query validation passed for project: {}", projectAbbr);
  }

  /**
   * Validates the main query string for dangerous patterns and wildcards.
   */
  private void validateQueryString(String query, List<String> violations) {
    if (query == null || query.isBlank()) {
      violations.add("Query string cannot be empty");
      return;
    }

    // Check length
    if (query.length() > MAX_QUERY_LENGTH) {
      violations.add("Query string exceeds maximum length of " + MAX_QUERY_LENGTH + " characters");
    }

    // Check for forbidden patterns
    for (Pattern pattern : FORBIDDEN_PATTERNS) {
      if (pattern.matcher(query).find()) {
        violations.add("Query contains forbidden pattern: " + pattern.pattern());
      }
    }

    // Check for expensive leading wildcards
    if (query.trim().startsWith("*")) {
      violations.add("Leading wildcards (*term) are prohibited due to performance concerns");
    }

    // Check for wildcard-only queries
    if (query.trim().equals("*:*") || query.trim().equals("*")) {
      violations.add("Wildcard-only queries (*:*) are prohibited. Use filter queries to narrow results.");
    }

    // Warn about wildcard usage (not blocking, but logged)
    if (query.contains("*") && query.split("\\*").length > 5) {
      log.warn("Query contains many wildcards, may be slow: {}", query);
    }
  }

  /**
   * Validates pagination parameters.
   */
  private void validatePagination(PlexusSearchQueryRequestDto request, List<String> violations) {
    Integer start = request.getStart();
    Integer rows = request.getRows();

    if (start != null && start < 0) {
      violations.add("Start offset must be non-negative");
    }

    if (rows != null && rows < 1) {
      violations.add("Rows must be at least 1");
    }

    if (rows != null && rows > MAX_ROWS_PER_QUERY) {
      violations.add("Cannot fetch more than " + MAX_ROWS_PER_QUERY + " rows per request. Use pagination or cursor marks.");
    }

    // Deep pagination warning
    if (start != null && start > MAX_DEEP_PAGINATION_OFFSET) {
      violations.add("Deep pagination beyond " + MAX_DEEP_PAGINATION_OFFSET +
          " is prohibited. Use cursor-based pagination (cursorMark) instead.");
    }

    // Cursor mark and start are mutually exclusive
    if (request.getCursorMark() != null && start != null && start > 0) {
      violations.add("Cannot use both cursorMark and start offset. Choose one pagination method.");
    }
  }

  /**
   * Validates filter queries.
   */
  private void validateFilterQueries(List<String> filterQueries, List<String> violations) {
    if (filterQueries == null) {
      return;
    }

    if (filterQueries.size() > MAX_FILTER_QUERIES) {
      violations.add("Cannot specify more than " + MAX_FILTER_QUERIES + " filter queries");
    }

    // Check each filter query for forbidden patterns
    for (String fq : filterQueries) {
      for (Pattern pattern : FORBIDDEN_PATTERNS) {
        if (pattern.matcher(fq).find()) {
          violations.add("Filter query contains forbidden pattern: " + pattern.pattern());
        }
      }
    }
  }

  /**
   * Validates faceting parameters.
   */
  private void validateFaceting(PlexusSearchQueryRequestDto request, List<String> violations) {
    List<String> facetFields = request.getFacetFields();

    if (facetFields != null && facetFields.size() > MAX_FACET_FIELDS) {
      violations.add("Cannot facet on more than " + MAX_FACET_FIELDS + " fields");
    }

    Integer facetLimit = request.getFacetLimit();
    if (facetLimit != null && (facetLimit < 1 || facetLimit > 100)) {
      violations.add("Facet limit must be between 1 and 100");
    }

    Integer facetMinCount = request.getFacetMinCount();
    if (facetMinCount != null && facetMinCount < 0) {
      violations.add("Facet min count must be non-negative");
    }
  }

  /**
   * Validates overall query complexity.
   */
  private void validateComplexity(PlexusSearchQueryRequestDto request, List<String> violations) {
    String query = request.getQuery();

    if (query == null) {
      return;
    }

    // Count boolean clauses (AND, OR, NOT)
    int andCount = countOccurrences(query, " AND ");
    int orCount = countOccurrences(query, " OR ");
    int notCount = countOccurrences(query, " NOT ");
    int totalClauses = andCount + orCount + notCount + 1; // +1 for base term

    if (totalClauses > MAX_BOOLEAN_CLAUSES) {
      violations.add("Query contains " + totalClauses + " boolean clauses, maximum is " + MAX_BOOLEAN_CLAUSES);
    }

    // Check for nested queries (parentheses depth)
    int maxDepth = calculateParenthesisDepth(query);
    if (maxDepth > 5) {
      violations.add("Query nesting depth (" + maxDepth + ") exceeds maximum of 5 levels");
    }
  }

  /**
   * Validates sort parameters.
   */
  private void validateSort(String sort, List<String> violations) {
    if (sort == null || sort.isBlank()) {
      return;
    }

    // Check for forbidden patterns in sort
    for (Pattern pattern : FORBIDDEN_PATTERNS) {
      if (pattern.matcher(sort).find()) {
        violations.add("Sort parameter contains forbidden pattern");
      }
    }

    // Validate format: field asc/desc
    String[] sortParts = sort.split(",");
    for (String part : sortParts) {
      part = part.trim();
      if (!part.matches("^[a-zA-Z0-9_]+ (asc|desc)$")) {
        violations.add("Invalid sort format: '" + part + "'. Expected 'field asc' or 'field desc'");
      }
    }
  }

  /**
   * Validates custom parameters (advanced use only).
   */
  private void validateCustomParams(java.util.Map<String, String> customParams, List<String> violations) {
    if (customParams == null || customParams.isEmpty()) {
      return;
    }

    // Block dangerous parameter names
    List<String> forbiddenParams = List.of(
        "stream.url", "stream.file", "stream.body", // Streaming attacks
        "qt", "shards", "collection" // Admin operations
    );

    for (String paramName : customParams.keySet()) {
      if (forbiddenParams.contains(paramName.toLowerCase())) {
        violations.add("Custom parameter '" + paramName + "' is forbidden");
      }
    }

    // Limit number of custom params
    if (customParams.size() > 10) {
      violations.add("Cannot specify more than 10 custom parameters");
    }
  }

  /**
   * Count occurrences of a substring (case-insensitive).
   */
  private int countOccurrences(String text, String substring) {
    if (text == null || substring == null) {
      return 0;
    }
    int count = 0;
    int index = 0;
    String lowerText = text.toLowerCase();
    String lowerSubstring = substring.toLowerCase();

    while ((index = lowerText.indexOf(lowerSubstring, index)) != -1) {
      count++;
      index += lowerSubstring.length();
    }
    return count;
  }

  /**
   * Calculate maximum parenthesis nesting depth.
   */
  private int calculateParenthesisDepth(String query) {
    int maxDepth = 0;
    int currentDepth = 0;

    for (char c : query.toCharArray()) {
      if (c == '(') {
        currentDepth++;
        maxDepth = Math.max(maxDepth, currentDepth);
      } else if (c == ')') {
        currentDepth--;
      }
    }

    return maxDepth;
  }
}
