package org.ddh.gamsapi.application.Integration.BaseSearch.Facet;

import lombok.extern.slf4j.Slf4j;
import org.ddh.gamsapi.application.Integration.BaseSearch.BaseSearchProperties;
import org.ddh.gamsapi.application.Integration.Common.exceptions.IntegrationDataProcessingException;
import org.springframework.data.domain.Pageable;
import org.springframework.util.MultiValueMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class FacetQueryBuilder {

  /**
   * Builds the full Solr faceted search URL.
   * @param coreName solr core to be queried
   * @param facetQuery the Solr query string (content of the "q" parameter)
   * @param facetFields the fields to facet on
   * @param pageable pagination and sorting info
   * @return the complete Solr faceted search URL
   */
  public static String buildSolrFacetUrl(
      String coreName,
      String facetQuery,
      Set<String> facetFields,
      Pageable pageable
  ) {

    // Build Solr request URL with faceting parameters
    StringBuilder url = new StringBuilder();
    url.append(String.format("/solr/%s/select", coreName));
    url.append("?q=").append(facetQuery);

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

    // ⭐ EXPLICIT field list - solr returns only the fields we specify here
    List<String> fieldsToReturn = List.of(
        BaseSearchProperties.OBJECT_ID.name,
        BaseSearchProperties.PROJECT.name,
        BaseSearchProperties.OBJECT_ID.name,
        BaseSearchProperties.DATASTREAMS.name,
        BaseSearchProperties.TYPE.name,
        BaseSearchProperties.TITLE.name,
        BaseSearchProperties.DESCRIPTION.name,
        BaseSearchProperties.CREATOR.name,
        BaseSearchProperties.PUBLISHER.name,
        BaseSearchProperties.RIGHTS.name,
        "dc.*"  // All Dublin Core fields
    );

    url.append("&fl=").append(String.join(",", fieldsToReturn));

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
    log.debug("Built solr faceted query: {}", finalUrl);

    return finalUrl;

  }


  /**
   * Builds Solr query string with project and Dublin Core filters.
   * Just what the query parameter needs.
   * Implements proper faceted search logic:
   * - Multiple values for SAME field = OR logic
   * - Different fields = AND logic
   */
  public static String buildSolrFacetQuery(
      Set<String> projectAbbrs,
      String fulltextQuery,
      MultiValueMap<String, String> selectedFacets
  ) {

    List<String> queryParts = new ArrayList<>();

    // STEP 1: Add fulltext query if provided
    if (fulltextQuery != null && !fulltextQuery.trim().isEmpty()) {
      String escapedFulltext = escapeSolrValue(fulltextQuery.trim());
      // Search in the objectFulltext field
      queryParts.add(String.format("%s:%s", BaseSearchProperties.FULLTEXT.name, escapedFulltext));
    }

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
  private static String normalizeDublinCoreFieldName(String dcFieldName) {
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
  private static String buildSolrFieldQuery(String fieldName, String value) {
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
  private static String escapeSolrValue(String value) {
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


}
