package org.ddh.gamsapi.application.Integration.GSearch.Facet;

import lombok.extern.slf4j.Slf4j;
import org.ddh.gamsapi.application.Integration.GSearch.GSearchProperties;
import org.ddh.gamsapi.application.Integration.Common.utils.solr.SolrUrlBuilder;
import org.ddh.gamsapi.application.Integration.Common.exceptions.IntegrationDataProcessingException;
import org.springframework.data.domain.Pageable;
import org.springframework.util.MultiValueMap;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class FacetQueryBuilder {


  /**
   * Builds a Solr faceted search URL with drill-down support.
   * TODO think about naming of method(s)
   * TODO propper testing?
   * @param solrCore solr core to be searched
   * @param projectAbbrs projects to be filtered
   * @param fulltextQuery fulltext search query
   * @param selectedFacets selected facets for filtering
   * @param facetFields fields to facet on
   * @param pageable pagination and sorting information
   * @return
   */
  public static String buildSolrFacetDrilldownUrl(
                                      String solrCore,
                                      Set<String> projectAbbrs,
                                      String fulltextQuery,
                                      MultiValueMap<String, String> selectedFacets,
                                      Set<String> facetFields,
                                      Pageable pageable) {

    // STEP 1: Build base query (q parameter for solr)
    // This is the foundation for drill-down faceting
    String baseQuery = buildBaseSolrQuery(projectAbbrs, fulltextQuery);

    // STEP 2: Build filter queries (fq) with tags for drill-down
    // Each facet filter gets its own fq parameter with a tag
    // Format: {!tag=type}dc.type:"Brief"
    List<String> filterQueries = buildSolrFilterQueries(selectedFacets);

    // STEP 3: Build complete Solr URL with drill-down exclusions
    // Each facet field will exclude its own tag: {!ex=type}dc.type
    // This allows seeing all values even when that facet is filtered
    return buildSolrFacetUrl(
        solrCore,
        baseQuery,
        filterQueries,
        facetFields,
        pageable
    );

  }

  /**
   * Builds a Solr faceted search URL with drill-down support.
   * @param coreName solr core that should be queried
   * @param baseQuery value of the q parameter for solr
   * @param filterQueries list of fq parameters for solr
   * @param facetFields set of facet fields to include
   * @param pageable pagination and sorting information
   * @return complete Solr URL for faceted search with drill-down (escaped and encoded)
   */
  public static String buildSolrFacetUrl(
      String coreName,
      String baseQuery,
      List<String> filterQueries,
      Set<String> facetFields,
      Pageable pageable
  ) {
    StringBuilder url = new StringBuilder();
    url.append(String.format("/solr/%s/select", coreName));

    // Base query (values should already be URL-encoded if needed)
    url.append("?q=").append(baseQuery);

    // Add filter queries (values already URL-encoded)
    for (String fq : filterQueries) {
      url.append("&fq=").append(fq);
    }

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

    // Field list
    List<String> fieldsToReturn = List.of(
        GSearchProperties.OBJECT_ID.name,
        GSearchProperties.PROJECT.name,
        GSearchProperties.DATASTREAMS.name,
        GSearchProperties.TYPE.name,
        GSearchProperties.TITLE.name,
        GSearchProperties.DESCRIPTION.name,
        GSearchProperties.CREATOR.name,
        GSearchProperties.PUBLISHER.name,
        GSearchProperties.RIGHTS.name,
        "dc.*"
    );
    url.append("&fl=").append(String.join(",", fieldsToReturn));

    // Faceting parameters
    url.append("&facet=true");
    url.append("&facet.mincount=1");
    url.append("&facet.limit=100");
    url.append("&facet.sort=count");

    // Add facet fields with exclusions
    for (String facetField : facetFields) {
      // Validate that facet field has proper format
      validateDublinCoreFieldName(facetField);

      String fieldShortName = extractFieldShortName(facetField);
      String facetFieldWithExclusion = String.format("{!ex=%s}%s", fieldShortName, facetField);
      url.append("&facet.field=").append(facetFieldWithExclusion);
    }

    url.append("&wt=json");
    url.append("&indent=true");

    // Final URL encoding for Solr special characters (solr uses this for certain operations)
    String finalUrl = SolrUrlBuilder.urlEncodeSolrSpecialCharacters(url.toString());

    return finalUrl;
  }

  /**
   * Builds the base Solr query (q parameter) for faceted search.
   * @param projectAbbrs set of project abbreviations to filter by
   * @param fulltextQuery fulltext search query (can be null or empty)
   * @return base Solr query string (escaped and URL-encoded)
   */
  public static String buildBaseSolrQuery(
      Set<String> projectAbbrs,
      String fulltextQuery
  ) {
    List<String> queryParts = new ArrayList<>();

    if (fulltextQuery != null && !fulltextQuery.trim().isEmpty()) {
      String escapedFulltext = SolrUrlBuilder.escapeSolrValue(fulltextQuery.trim());
      // URL encode the fulltext value
      String encodedFulltext = urlEncode(escapedFulltext);
      queryParts.add(String.format("%s:%s", GSearchProperties.FULLTEXT.name, encodedFulltext));
    }

    if (projectAbbrs.size() == 1) {
      String project = SolrUrlBuilder.escapeSolrValue(projectAbbrs.iterator().next());
      queryParts.add(String.format("%s:%s", GSearchProperties.PROJECT.name, project));
    } else {
      String projectQuery = projectAbbrs.stream()
          .map(abbr -> String.format("%s:%s",
              GSearchProperties.PROJECT.name,
              SolrUrlBuilder.escapeSolrValue(abbr)))
          .collect(Collectors.joining(" OR "));
      queryParts.add("(" + projectQuery + ")");
    }

    String finalQuery = queryParts.isEmpty() ? "*:*" : String.join(" AND ", queryParts);
    log.debug("Built base Solr query: {}", finalQuery);
    return finalQuery;
  }

  /**
   * Builds Solr filter queries (fq parameters) for selected facets with drill-down tags.
   * @param selectedFacets map of selected facets (Dublin Core field name to list of values)
   * @return list of Solr filter query strings (escaped and URL-encoded)
   */
  public static List<String> buildSolrFilterQueries(
      MultiValueMap<String, String> selectedFacets
  ) {
    List<String> filterQueries = new ArrayList<>();

    if (selectedFacets == null || selectedFacets.isEmpty()) {
      return filterQueries;
    }

    selectedFacets.forEach((dcField, values) -> {
      if (values != null && !values.isEmpty()) {
        // VALIDATION: Ensure field starts with "dc."
        validateDublinCoreFieldName(dcField);

        String fieldShortName = extractFieldShortName(dcField);

        if (values.size() == 1) {
          // Single value: {!tag=type}dc.type:encodedValue
          String fq = String.format("{!tag=%s}%s",
              fieldShortName,
              buildSolrFieldQuery(dcField, values.get(0)));
          filterQueries.add(fq);
        } else {
          // Multiple values: {!tag=type}(dc.type:val1 OR dc.type:val2)
          String valueQuery = values.stream()
              .map(value -> buildSolrFieldQuery(dcField, value))
              .collect(Collectors.joining(" OR "));
          String fq = String.format("{!tag=%s}(%s)", fieldShortName, valueQuery);
          filterQueries.add(fq);
        }
      }
    });

    log.debug("Built {} filter queries for drill-down", filterQueries.size());
    return filterQueries;
  }

  /**
   * Extracts the short field name from a full Dublin Core field name.
   * E.g. "dc.type" -> "type"
   *
   * @param fullFieldName full Dublin Core field name (must start with "dc.")
   * @return short field name without prefix
   * @throws IntegrationDataProcessingException if field doesn't start with "dc."
   */
  private static String extractFieldShortName(String fullFieldName) {
    if (!fullFieldName.startsWith("dc.")) {
      throw new IntegrationDataProcessingException(
          "Cannot extract short name from field '" + fullFieldName +
              "' - must start with 'dc.' prefix"
      );
    }
    return fullFieldName.substring(3);
  }

  /**
   * Builds a Solr field query with proper escaping AND URL encoding.
   *
   * CRITICAL: URL-encodes the value so special characters like quotes
   * don't cause "Illegal character" errors in URIs.
   */
  public static String buildSolrFieldQuery(String fieldName, String value) {
    if (value == null || value.trim().isEmpty()) {
      throw new IntegrationDataProcessingException("Search value cannot be null or empty");
    }

    // STEP 1: Escape for Solr syntax
    String escapedValue = SolrUrlBuilder.escapeSolrValue(value.trim());

    // STEP 2: Wrap in quotes for phrase matching
    // CRITICAL: Quotes ensure multi-word values are treated as phrases
    String quotedValue = "\"" + escapedValue + "\"";

    // STEP 3: URL-encode to handle special characters
    String urlEncodedValue = urlEncode(quotedValue);

    // STEP 4: Build query - value is now quoted and URL-encoded
    return String.format("%s:%s", fieldName, urlEncodedValue);
  }

  /**
   * URL-encodes a string for safe use in URLs.
   * Converts special characters like " to %22, \ to %5C, etc.
   */
  private static String urlEncode(String value) {
    try {
      return URLEncoder.encode(value, StandardCharsets.UTF_8.toString());
    } catch (UnsupportedEncodingException e) {
      throw new IntegrationDataProcessingException("Failed to URL-encode value: " + value);
    }
  }

  /**
   * Validates that a Dublin Core field name starts with "dc." prefix.
   * Throws exception if invalid - NO automatic fixing/normalization.
   *
   * @param dcFieldName Dublin Core field name (must start with "dc.")
   * @throws IntegrationDataProcessingException if field name is invalid
   */
  private static void validateDublinCoreFieldName(String dcFieldName) {
    if (dcFieldName == null || dcFieldName.isEmpty()) {
      throw new IntegrationDataProcessingException(
          "Dublin Core field name cannot be null or empty"
      );
    }

    if (!dcFieldName.startsWith("dc.")) {
      String msg = String.format(
          "Invalid Dublin Core field name: '%s'. Must start with 'dc.' prefix. " +
              "This indicates a bug in the controller layer - field names should be " +
              "validated and filtered before reaching the query builder.",
          dcFieldName
      );
      log.error(msg);
      throw new IntegrationDataProcessingException(msg);
    }
  }



}
