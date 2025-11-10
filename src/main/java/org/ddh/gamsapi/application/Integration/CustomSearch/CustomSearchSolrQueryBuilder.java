package org.ddh.gamsapi.application.Integration.CustomSearch;

import lombok.extern.slf4j.Slf4j;
import org.ddh.gamsapi.application.Integration.GSearch.BaseSearchProperties;
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
   * TODO
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
    url.append("&hl.fl=").append(BaseSearchProperties.FULLTEXT.name); // Highlight fulltext field
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
