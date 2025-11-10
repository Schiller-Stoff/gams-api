package org.ddh.gamsapi.application.Integration.CustomSearch;

import lombok.extern.slf4j.Slf4j;
import org.ddh.gamsapi.application.Integration.GSearch.GSearchProperties;
import org.ddh.gamsapi.application.Integration.GSearch.Fulltext.FulltextSolrConfig;
import org.ddh.gamsapi.application.Integration.Common.utils.solr.SolrUrlBuilder;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class CustomSearchSolrQueryBuilder {

  /**
   * TODO jdoc
   * TODO test
   * @param projectAbbrs
   * @param fulltextQuery
   * @return
   */
  public static String buildBaseSolrQuery(
      Set<String> projectAbbrs,
      String fulltextQuery
  ){
    List<String> queryParts = new ArrayList<>();

    if (fulltextQuery != null && !fulltextQuery.trim().isEmpty()) {
      String escapedFulltext = SolrUrlBuilder.escapeSolrValue(fulltextQuery.trim());
      // URL encode the fulltext value
      String encodedFulltext = SolrUrlBuilder.urlEncode(escapedFulltext);
      queryParts.add(String.format("%s:%s", CustomSearchProperties.SOLR_FULLTEXT_PROPERTY.name, encodedFulltext));
    }

    if(projectAbbrs.isEmpty()){
      queryParts.add(String.format("%s:*", CustomSearchProperties.ENTITY_PROJECT_ABBR.name));
    } else if (projectAbbrs.size() == 1) {
      String project = SolrUrlBuilder.escapeSolrValue(projectAbbrs.iterator().next());
      queryParts.add(String.format("%s:%s", CustomSearchProperties.ENTITY_PROJECT_ABBR.name, project));
    } else {
      String projectQuery = projectAbbrs.stream()
          .map(abbr -> String.format("%s:%s",
              CustomSearchProperties.ENTITY_PROJECT_ABBR.name,
              SolrUrlBuilder.escapeSolrValue(abbr)))
          .collect(Collectors.joining(" OR "));
      queryParts.add("(" + projectQuery + ")");
    }

    String finalQuery = queryParts.isEmpty() ? "*:*" : String.join(" AND ", queryParts);
    log.debug("Built base Solr query: {}", finalQuery);
    return finalQuery;
  }

  /**
   * Builds Solr filter queries from tag criteria.
   * @param tags tag filters
   * @return list of Solr filter queries based on given tags
   */
  public static List<String> buildFilterQueries(
      List<String> tags
  ) {

    if(tags == null || tags.isEmpty()){
      return List.of();
    }

    List<String> filterQueries = new ArrayList<>();
    if (tags.size() == 1) {
      String tagValue = SolrUrlBuilder.escapeSolrValue(tags.get(0));
      filterQueries.add(String.format("%s:%s", CustomSearchProperties.ENTITY_TAGS.name, tagValue));
    } else {
      String tagsFq = tags.stream()
          .map(tag -> String.format("%s:%s",
              CustomSearchProperties.ENTITY_TAGS.name,
              SolrUrlBuilder.escapeSolrValue(tag)))
          .collect(Collectors.joining(" AND "));
      filterQueries.add("(" + tagsFq + ")");
    }


    return filterQueries;

  }
  /**
   * Builds date range filter queries.
   *
   * @param startDate Optional start date filter (ISO-8601 format with timezone)
   * @param endDate Optional end date filter (ISO-8601 format with timezone)
   * @return List of Solr date range filter queries
   */
  public static List<String> buildDateFilterQueries(
      String startDate,
      String endDate
  ) {
    List<String> dateFilters = new ArrayList<>();

    if (startDate != null && !startDate.trim().isEmpty()) {
      // DO NOT escape ISO-8601 dates - they contain valid Solr syntax
      // URL encoding will be handled by the HTTP client
      dateFilters.add(String.format(
          "%s:[%s TO *]",
          CustomSearchProperties.ENTITY_END_DATE.name,
          startDate.trim()  // ✅ Remove escapeSolrValue()
      ));
    }

    if (endDate != null && !endDate.trim().isEmpty()) {
      dateFilters.add(String.format(
          "%s:[* TO %s]",
          CustomSearchProperties.ENTITY_START_DATE.name,
          endDate.trim()  // ✅ Remove escapeSolrValue()
      ));
    }

    return dateFilters;
  }

  /**
   * TODO jdoc
   * TODO test
   * @param coreName
   * @param baseQuery
   * @param filterQueries
   * @param pageable
   * @return
   */
  public static String buildSolrUrl(
      String coreName,
      String baseQuery,
      List<String> filterQueries,
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
//    List<String> fieldsToReturn = List.of(
//        "*" // return all fields
//    );
//    url.append("&fl=").append(String.join(",", fieldsToReturn));


    // ========== HIGHLIGHTING PARAMETERS ==========
    url.append("&hl=true");  // Enable highlighting
    // TODO remove using reference to GSearchProperties here
    url.append("&hl.fl=").append(GSearchProperties.FULLTEXT.name); // Highlight fulltext field
    url.append("&hl.requireFieldMatch=true");  // Only highlight if field matches query (only objectFulltext being here matched)
    url.append("&hl.snippets=3");  // Max 3 snippets per field
    url.append("&hl.fragsize=150");  // ~150 chars per snippet
    url.append("&hl.simple.pre=").append(SolrUrlBuilder.urlEncode(FulltextSolrConfig.HIGHLIGHT_PRE.name));  // HTML5 <mark> tag
    url.append("&hl.simple.post=").append(SolrUrlBuilder.urlEncode(FulltextSolrConfig.HIGHLIGHT_POST.name));
    url.append("&hl.method=unified");  // Use unified highlighter (best performance + accuracy)
    url.append("&hl.tag.pre=").append(SolrUrlBuilder.urlEncode(FulltextSolrConfig.HIGHLIGHT_PRE.name));
    url.append("&hl.tag.post=").append(SolrUrlBuilder.urlEncode(FulltextSolrConfig.HIGHLIGHT_POST.name));
    // ====================================================


    url.append("&wt=json");
    url.append("&indent=true");

    // Final URL encoding for Solr special characters (solr uses this for certain operations)
    String finalUrl = SolrUrlBuilder.urlEncodeSolrSpecialCharacters(url.toString());

    return finalUrl;

  }


}
