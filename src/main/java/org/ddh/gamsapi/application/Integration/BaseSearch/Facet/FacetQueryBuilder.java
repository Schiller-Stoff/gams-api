package org.ddh.gamsapi.application.Integration.BaseSearch.Facet;

import lombok.extern.slf4j.Slf4j;
import org.ddh.gamsapi.application.Integration.BaseSearch.BaseSearchProperties;
import org.ddh.gamsapi.application.Integration.BaseSearch.solr.SolrGamsCores;
import org.ddh.gamsapi.application.Integration.BaseSearch.solr.SolrUrlBuilder;
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

  public static String buildSolrCountUrl(
      Set<String> projectAbbrs
  ) {

    StringBuilder url = new StringBuilder();
    url.append(String.format("/solr/%s/select", SolrGamsCores.GAMS_CORE.value));
    url.append("?q=*:*");

    // Project filter
    if (projectAbbrs.size() == 1) {
      url.append(String.format("&q=%s:%s",
          BaseSearchProperties.PROJECT.name,
          SolrUrlBuilder.escapeSolrValue(projectAbbrs.iterator().next())));
    } else {
      String projectQuery = projectAbbrs.stream()
          .map(abbr -> String.format("%s:%s",
              BaseSearchProperties.PROJECT.name,
              SolrUrlBuilder.escapeSolrValue(abbr)))
          .collect(Collectors.joining(" OR "));
      url.append("&q=(").append(projectQuery).append(")");
    }

    // We only need the count
    url.append("&rows=0");
    url.append("&wt=json");
    url.append("&indent=true");

    String finalUrl = url.toString();
    log.debug("Built solr count query: {}", finalUrl);

    return finalUrl;

  }

  /**
   * Builds a Solr faceted search URL with drill-down support.
   * @param coreName todo JDOC
   * @param baseQuery
   * @param filterQueries
   * @param facetFields
   * @param pageable
   * @return
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
        BaseSearchProperties.OBJECT_ID.name,
        BaseSearchProperties.PROJECT.name,
        BaseSearchProperties.DATASTREAMS.name,
        BaseSearchProperties.TYPE.name,
        BaseSearchProperties.TITLE.name,
        BaseSearchProperties.DESCRIPTION.name,
        BaseSearchProperties.CREATOR.name,
        BaseSearchProperties.PUBLISHER.name,
        BaseSearchProperties.RIGHTS.name,
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
      String fieldShortName = extractFieldShortName(facetField);
      String facetFieldWithExclusion = String.format("{!ex=%s}%s", fieldShortName, facetField);
      url.append("&facet.field=").append(facetFieldWithExclusion);
    }

    url.append("&wt=json");
    url.append("&indent=true");

    return url.toString();
  }

  public static String buildBaseSolrQuery(
      Set<String> projectAbbrs,
      String fulltextQuery
  ) {
    List<String> queryParts = new ArrayList<>();

    if (fulltextQuery != null && !fulltextQuery.trim().isEmpty()) {
      String escapedFulltext = SolrUrlBuilder.escapeSolrValue(fulltextQuery.trim());
      // URL encode the fulltext value
      String encodedFulltext = urlEncode(escapedFulltext);
      queryParts.add(String.format("%s:%s", BaseSearchProperties.FULLTEXT.name, encodedFulltext));
    }

    if (projectAbbrs.size() == 1) {
      String project = SolrUrlBuilder.escapeSolrValue(projectAbbrs.iterator().next());
      queryParts.add(String.format("%s:%s", BaseSearchProperties.PROJECT.name, project));
    } else {
      String projectQuery = projectAbbrs.stream()
          .map(abbr -> String.format("%s:%s",
              BaseSearchProperties.PROJECT.name,
              SolrUrlBuilder.escapeSolrValue(abbr)))
          .collect(Collectors.joining(" OR "));
      queryParts.add("(" + projectQuery + ")");
    }

    String finalQuery = queryParts.isEmpty() ? "*:*" : String.join(" AND ", queryParts);
    log.debug("Built base Solr query: {}", finalQuery);
    return finalQuery;
  }

  public static List<String> buildSolrFilterQueries(
      MultiValueMap<String, String> selectedFacets
  ) {
    List<String> filterQueries = new ArrayList<>();

    if (selectedFacets == null || selectedFacets.isEmpty()) {
      return filterQueries;
    }

    selectedFacets.forEach((dcField, values) -> {
      if (values != null && !values.isEmpty()) {
        String solrFieldName = normalizeDublinCoreFieldName(dcField);
        String fieldShortName = extractFieldShortName(solrFieldName);

        if (values.size() == 1) {
          // Single value: {!tag=type}dc.type:encodedValue
          String fq = String.format("{!tag=%s}%s",
              fieldShortName,
              buildSolrFieldQuery(solrFieldName, values.get(0)));
          filterQueries.add(fq);
        } else {
          // Multiple values: {!tag=type}(dc.type:val1 OR dc.type:val2)
          String valueQuery = values.stream()
              .map(value -> buildSolrFieldQuery(solrFieldName, value))
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
   * TODO
   * @param dcFieldName
   * @return
   */
  private static String normalizeDublinCoreFieldName(String dcFieldName) {
    if (dcFieldName == null || dcFieldName.isEmpty()) {
      throw new IntegrationDataProcessingException("Dublin Core field name cannot be null or empty");
    }
    if (dcFieldName.startsWith("dc.")) {
      return dcFieldName;
    }
    return "dc." + dcFieldName;
  }

  /**
   * TODO
   * @param fullFieldName
   * @return
   */
  private static String extractFieldShortName(String fullFieldName) {
    if (fullFieldName.startsWith("dc.")) {
      return fullFieldName.substring(3);
    }
    return fullFieldName;
  }

  /**
   * Builds a Solr field query with proper escaping AND URL encoding.
   *
   * CRITICAL: URL-encodes the value so special characters like quotes
   * don't cause "Illegal character" errors in URIs.
   */
  private static String buildSolrFieldQuery(String fieldName, String value) {
    if (value == null || value.trim().isEmpty()) {
      throw new IntegrationDataProcessingException("Search value cannot be null or empty");
    }

    // TODO test / think about

    // STEP 1: Escape for Solr syntax (adds quotes around value)
    String escapedValue = SolrUrlBuilder.escapeSolrValue(value.trim());

    // STEP 2: URL-encode to handle special characters like quotes, backslashes
    String urlEncodedValue = urlEncode(escapedValue);

    // STEP 3: Build query - value is already quoted and URL-encoded
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



}
