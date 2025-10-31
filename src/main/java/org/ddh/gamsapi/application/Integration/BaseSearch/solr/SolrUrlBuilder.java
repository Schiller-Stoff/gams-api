package org.ddh.gamsapi.application.Integration.BaseSearch.solr;

import lombok.extern.slf4j.Slf4j;
import org.ddh.gamsapi.application.Integration.BaseSearch.BaseSearchProperties;
import org.ddh.gamsapi.application.Integration.BaseSearch.Fulltext.FulltextSolrConfig;
import org.ddh.gamsapi.application.Integration.Common.exceptions.IntegrationDataProcessingException;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class SolrUrlBuilder {

  /**
   * Escapes special characters in Solr query values.
   * CRITICAL: Must properly escape to prevent query syntax errors.
   * TODO TEST ME
   */
  public static String escapeSolrValue(String value) {
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
   * URL-encodes Solr special characters in query values.
   * CRITICAL: Must URL-encode to prevent "Illegal character" errors in URIs
   * @param url The input string to encode
   * @return The URL-encoded value
   */
  public static String urlEncodeSolrSpecialCharacters(String url) {
    if (url == null) {
      return "";
    }

    return url
        .replace("{", "%7B")
        .replace("}", "%7D")
        .replace("|", "%7C")
        .replace("!", "%21")
        .replace(" ", "%20");
  }

  /**
   * Builds the base Solr query string with project abbreviations and fulltext query.
   * @param projectAbbrs Set of project abbreviations to filter by. If empty includes all projects.
   * @param fulltextQuery Fulltext search query - if empty finds everything
   * @return
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
      queryParts.add(String.format("%s:%s", BaseSearchProperties.FULLTEXT.name, encodedFulltext));
    }

    if(projectAbbrs.isEmpty()){
      queryParts.add(String.format("%s:*", BaseSearchProperties.PROJECT.name));
    } else if (projectAbbrs.size() == 1) {
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


  /**
   * URL-encodes a string for safe use in URLs.
   * Converts special characters like " to %22, \ to %5C, etc.
   */
  public static String urlEncode(String value) {
    try {
      return URLEncoder.encode(value, StandardCharsets.UTF_8.toString());
    } catch (UnsupportedEncodingException e) {
      throw new IntegrationDataProcessingException("Failed to URL-encode value: " + value);
    }
  }

  /**
   * Builds a Solr field query with mode-aware field selection and proper escaping.
   *
   * @param fieldName The base Dublin Core field name (e.g., "dc.subject" or "subject")
   * @param value The search value
   * @return Fully constructed and encoded field query
   * @throws IntegrationDataProcessingException if value is null/empty
   */
  public static String buildSolrFieldQuery(
      String fieldName,
      String value
  ) {
    if (value == null || value.trim().isEmpty()) {
      throw new IntegrationDataProcessingException("Search value cannot be null or empty");
    }

    // TODO rethink if the procedure is correct here (assigning custom fields per facet / fulltext should be in correspondent packages)

    // TODO is this normalization workflow needed here?
    // Normalize field name (ensure "dc." prefix)
    String normalizedField = fieldName.startsWith("dc.")
        ? fieldName
        : "dc." + fieldName;

    // TODO not very elegant
    String solrField = normalizedField;

    String queryValue;
    if(fieldName.endsWith(FulltextSolrConfig.PHRASE_SEARCH_SUFFIX.name)){
      // remove as Phrase suffix
      solrField = solrField.replace(FulltextSolrConfig.PHRASE_SEARCH_SUFFIX.name, "");
      // Always use PHRASE mode for fields ending with "AsPhrase"
      String escapedValue = escapeSolrValue(value.trim());
      queryValue = urlEncode(escapedValue);
      log.trace("Built AS_PHRASE query: {}:{}", solrField, queryValue);
      return String.format("%s:%s", solrField, queryValue);
    } else {
      // built as word search
      // SUBSTRING mode: No escaping, just URL encode (let Solr tokenize)
      // This allows "Tag" to match "Tagsatzung" after tokenization
      // TODO following procedure must be made more transparent! (capture in enum)
      solrField = String.format("%s_txt", normalizedField);
      queryValue = urlEncode(value.trim());
      log.trace("Built SUBSTRING query: {}:{}", solrField, queryValue);
    }

    return String.format("%s:%s", solrField, queryValue);
  }

}
