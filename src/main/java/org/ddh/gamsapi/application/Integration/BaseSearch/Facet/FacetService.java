package org.ddh.gamsapi.application.Integration.BaseSearch.Facet;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ddh.gamsapi.application.Integration.BaseSearch.BaseSearchFacetResponse;
import org.ddh.gamsapi.application.Integration.BaseSearch.BaseSearchProperties;
import org.ddh.gamsapi.application.Integration.BaseSearch.solr.SolrClient;
import org.ddh.gamsapi.application.Integration.BaseSearch.solr.SolrFacetedResponse;
import org.ddh.gamsapi.application.Integration.BaseSearch.solr.SolrGamsCores;
import org.ddh.gamsapi.application.Integration.Common.exceptions.IntegrationDataProcessingException;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service for performing faceted searches on Dublin Core metadata in Solr.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class FacetService {


  private final SolrClient solrClient;

  /**
   * Performs faceted Dublin Core search in Solr.
   * Uses the updated schema with "dc.fieldname" format where all language variants
   * are stored in a single multi-valued field (e.g., dc.title = ["english title", "german title"])
   *
   * @param projectAbbrs Set of project abbreviations to filter by
   * @param selectedFacets MultiValueMap of selected Dublin Core facets (field -> values)
   * @param pageable Pagination information
   * @return FacetSearchResponse containing results, facets, and metadata
   */
  public BaseSearchFacetResponse facetSearch(
      Set<String> projectAbbrs,
      MultiValueMap<String, String> selectedFacets,
      Pageable pageable) {

    long startTime = System.currentTimeMillis();

    log.debug("Solr faceted search: projects={}, filters={}, page={}",
        projectAbbrs, selectedFacets, pageable);

    // Validate inputs
    if (projectAbbrs == null || projectAbbrs.isEmpty()) {
      throw new IntegrationDataProcessingException("Project abbreviations must not be empty");
    }

    // STEP 1: Build Solr query with filters
    String solrQuery = buildSolrFacetQuery(projectAbbrs, selectedFacets);

    // STEP 2: Define default facet fields (Dublin Core standard fields with "dc." prefix)
    // TODO: is this necessary?
    Set<String> facetFields = getDefaultDublinCoreFacetFields();

    // STEP 3: Execute Solr search with faceting
    String solrResponse = executeSolrFacetedSearch(
        SolrGamsCores.GAMS_CORE.value,
        solrQuery,
        facetFields,
        pageable
    );

    // STEP 4: Parse Solr response
    SolrFacetedResponse parsedResponse = SolrFacetedResponse.from(solrResponse);

    long totalTime = System.currentTimeMillis() - startTime;

    log.info("Solr faceted search completed in {}ms - found {} results with {} facet fields",
        totalTime, parsedResponse.getNumFound(), facetFields.size());


    // STEP 5: Transform to response from our API

    return BaseSearchFacetResponse.from(
        parsedResponse, selectedFacets
    );

  }


  /**
   * Builds Solr query string with project and Dublin Core filters.
   * Implements proper faceted search logic:
   * - Multiple values for SAME field = OR logic
   * - Different fields = AND logic
   */
  private String buildSolrFacetQuery(
      Set<String> projectAbbrs,
      MultiValueMap<String, String> selectedFacets) {

    List<String> queryParts = new ArrayList<>();

    // Add project filter (required)
    if (projectAbbrs.size() == 1) {
      queryParts.add(String.format("%s:%s",
          BaseSearchProperties.PROJECT.name,
          escapeSolrValue(projectAbbrs.iterator().next())));
    } else {
      String projectQuery = projectAbbrs.stream()
          .map(abbr -> String.format("%s:%s",
              BaseSearchProperties.PROJECT.name,
              escapeSolrValue(abbr)))
          .collect(Collectors.joining(" OR "));
      queryParts.add("(" + projectQuery + ")");
    }

    // Add Dublin Core facet filters
    if (selectedFacets != null && !selectedFacets.isEmpty()) {
      selectedFacets.forEach((dcField, values) -> {
        if (values != null && !values.isEmpty()) {
          // Map DC field name to Solr field name (dc.title, dc.creator, etc.)
          String solrFieldName = normalizeDublinCoreFieldName(dcField);

          // Build OR query for multiple values of same field
          if (values.size() == 1) {
            // Single value - simple query
            queryParts.add(buildSolrFieldQuery(solrFieldName, values.get(0)));
          } else {
            // Multiple values - OR query
            String fieldQuery = values.stream()
                .map(value -> buildSolrFieldQuery(solrFieldName, value))
                .collect(Collectors.joining(" OR "));
            queryParts.add("(" + fieldQuery + ")");
          }
        }
      });
    }

    // Combine all parts with AND
    String finalQuery = queryParts.isEmpty() ? "*:*" : String.join(" AND ", queryParts);

    log.debug("Built Solr query: {}", finalQuery);
    return finalQuery;
  }

  /**
   * Normalizes Dublin Core field names to Solr schema format.
   * Ensures consistent "dc.fieldname" format.
   *
   * Examples:
   * - "title" -> "dc.title"
   * - "dc.title" -> "dc.title"
   * - "creator" -> "dc.creator"
   */
  private String normalizeDublinCoreFieldName(String dcFieldName) {
    if (dcFieldName == null || dcFieldName.isEmpty()) {
      throw new IntegrationDataProcessingException("Dublin Core field name cannot be null or empty");
    }

    // Already has "dc." prefix
    if (dcFieldName.startsWith("dc.")) {
      return dcFieldName;
    }

    // Add "dc." prefix
    return "dc." + dcFieldName;
  }

  /**
   * Builds a Solr field query with proper escaping.
   * Handles multi-valued fields where all language variants are in one field.
   */
  private String buildSolrFieldQuery(String fieldName, String value) {
    if (value == null || value.trim().isEmpty()) {
      throw new IntegrationDataProcessingException("Search value cannot be null or empty");
    }

    String escapedValue = escapeSolrValue(value.trim());

    // For text fields, use exact phrase matching
    // This works well with multi-valued fields containing different language variants
    return String.format("%s:\"%s\"", fieldName, escapedValue);
  }

  /**
   * Escapes special characters in Solr query values.
   * CRITICAL: Must properly escape to prevent query syntax errors.
   */
  private String escapeSolrValue(String value) {
    if (value == null) {
      return "";
    }

    // Escape Solr special characters: + - && || ! ( ) { } [ ] ^ " ~ * ? : \ /
    return value
        .replace("\\", "\\\\")  // Backslash FIRST
        .replace("\"", "\\\"")  // Quote
        .replace("+", "\\+")
        .replace("-", "\\-")
        .replace("&&", "\\&&")
        .replace("||", "\\||")
        .replace("!", "\\!")
        .replace("(", "\\(")
        .replace(")", "\\)")
        .replace("{", "\\{")
        .replace("}", "\\}")
        .replace("[", "\\[")
        .replace("]", "\\]")
        .replace("^", "\\^")
        .replace("~", "\\~")
        .replace("*", "\\*")
        .replace("?", "\\?")
        .replace(":", "\\:");
  }

  /**
   * Returns default Dublin Core fields for faceting.
   * Uses schema format with "dc." prefix.
   *
   * Based on your schema definition and common faceting needs.
   */
  private Set<String> getDefaultDublinCoreFacetFields() {
    return Set.of(
        "dc.coverage",    // Geographic/temporal coverage - commonly faceted
        "dc.type",        // Resource type - commonly faceted
        "dc.creator",     // Creator/author - commonly faceted
        "dc.subject",     // Subject/keywords - commonly faceted
        "dc.language",    // Language - commonly faceted
        "dc.format",      // Format - commonly faceted
        "dc.publisher"    // Publisher - useful for faceting
    );
  }

  /**
   * Executes Solr faceted search query.
   * Returns raw JSON response from Solr.
   */
  private String executeSolrFacetedSearch(
      String coreName,
      String query,
      Set<String> facetFields,
      Pageable pageable) {

    // Build Solr request URL with faceting parameters
    StringBuilder url = new StringBuilder();
    url.append(String.format("/solr/%s/select", coreName));
    url.append("?q=").append(query);

    // Pagination
    url.append("&start=").append(pageable.getOffset());
    url.append("&rows=").append(pageable.getPageSize());

    // Sorting
    if (pageable.getSort().isSorted()) {
      String sortParam = pageable.getSort().stream()
          .map(order -> order.getProperty() + " " + order.getDirection().name().toLowerCase())
          .collect(Collectors.joining(","));
      url.append("&sort=").append(sortParam);
    }

    // Faceting parameters
    url.append("&facet=true");
    url.append("&facet.mincount=1"); // Only return facets with at least 1 doc
    url.append("&facet.limit=100");  // Max facet values per field
    url.append("&facet.sort=count"); // Sort by count (most common first)

    // Add facet fields
    for (String facetField : facetFields) {
      url.append("&facet.field=").append(facetField);
    }

    // Response format
    url.append("&wt=json");
    url.append("&indent=true");

    String finalUrl = url.toString();
    log.debug("Executing Solr faceted query: {}", finalUrl);

    return solrClient.get(finalUrl);
  }


}
