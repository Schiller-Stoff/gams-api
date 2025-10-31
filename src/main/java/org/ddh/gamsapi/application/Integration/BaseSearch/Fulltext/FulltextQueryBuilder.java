package org.ddh.gamsapi.application.Integration.BaseSearch.Fulltext;

import lombok.extern.slf4j.Slf4j;
import org.ddh.gamsapi.application.Integration.BaseSearch.BaseSearchProperties;
import org.ddh.gamsapi.application.Integration.BaseSearch.DublinCoreSearchMode;
import org.ddh.gamsapi.application.Integration.BaseSearch.solr.SolrUrlBuilder;
import org.springframework.data.domain.Pageable;
import org.springframework.util.MultiValueMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Builder for constructing Solr queries for fulltext search with Dublin Core filters.
 */
@Slf4j
public class FulltextQueryBuilder {

  /**
   * Builds the base Solr query string with project abbreviations and fulltext query.
   * @param projectAbbrs Set of project abbreviations to filter by
   * @param fulltextQuery Fulltext search query - if empty finds everything
   * @return Base Solr query string (value of solr's "q" parameter)
   */
  public static String buildBaseSolrQuery(
      Set<String> projectAbbrs,
      String fulltextQuery
  ) {
    return SolrUrlBuilder.buildBaseSolrQuery(projectAbbrs, fulltextQuery);
  }


  /**
   * Builds Solr filter queries from Dublin Core criteria with mode-specific matching.
   *
   * <p><b>Filter Query Structure:</b></p>
   * <ul>
   *   <li>Single value: {@code dc.field:value}</li>
   *   <li>Multiple values: {@code (dc.field:val1 OR dc.field:val2)}</li>
   * </ul>
   *
   * <p><b>Mode Behavior:</b></p>
   * <table border="1">
   *   <tr>
   *     <th>Mode</th>
   *     <th>Field</th>
   *     <th>Example</th>
   *   </tr>
   *   <tr>
   *     <td>PHRASE</td>
   *     <td>dc.subject</td>
   *     <td>dc.subject:"Tag"</td>
   *   </tr>
   *   <tr>
   *     <td>SUBSTRING</td>
   *     <td>dc.subject_txt</td>
   *     <td>dc.subject_txt:Tag</td>
   *   </tr>
   * </table>
   *
   * @param selectedFacets MultiValueMap of DC field filters (field -> values)
   * @param mode Search mode determining field selection and escaping
   * @return List of filter query strings
   */
  public static List<String> buildSolrFilterQueries(
      MultiValueMap<String, String> selectedFacets,
      DublinCoreSearchMode mode
  ) {
    List<String> filterQueries = new ArrayList<>();

    if (selectedFacets == null || selectedFacets.isEmpty()) {
      return filterQueries;
    }

    selectedFacets.forEach((dcField, values) -> {
      if (values != null && !values.isEmpty()) {
        if (values.size() == 1) {
          // Single value filter
          String fq = SolrUrlBuilder.buildSolrFieldQuery(dcField, values.get(0), mode);
          filterQueries.add(fq);
        } else {
          // Multiple values: OR them together
          String valueQuery = values.stream()
              .map(value -> SolrUrlBuilder.buildSolrFieldQuery(dcField, value, mode))
              .collect(Collectors.joining(" OR "));
          filterQueries.add("(" + valueQuery + ")");
        }
      }
    });

    log.debug("Built {} filter queries with mode {} for fulltext search",
        filterQueries.size(), mode);
    return filterQueries;
  }

  /**
   * Builds a Solr faceted search URL with drill-down support.
   * TODO test?
   * @param coreName Solr core name
   * @param baseQuery Base query string (value of solr's "q" parameter)
   * @param filterQueries List of filter queries (values of solr's "fq" parameters)
   * @param pageable Pagination and sorting information
   * @return Complete Solr URL for fulltext search with filters
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


    // ========== HIGHLIGHTING PARAMETERS (NEW) ==========
    url.append("&hl=true");  // Enable highlighting
    url.append("&hl.fl=").append(BaseSearchProperties.FULLTEXT.name); // Highlight fulltext field
    url.append("&hl.requireFieldMatch=true");  // Only highlight if field matches query (only objectFulltext being here matched)
    url.append("&hl.snippets=3");  // Max 3 snippets per field
    url.append("&hl.fragsize=150");  // ~150 chars per snippet
    url.append("&hl.simple.pre=").append(SolrUrlBuilder.urlEncode(FulltextResponseProperties.HIGHLIGHT_PRE.name));  // HTML5 <mark> tag
    url.append("&hl.simple.post=").append(SolrUrlBuilder.urlEncode(FulltextResponseProperties.HIGHLIGHT_POST.name));
    url.append("&hl.method=unified");  // Use unified highlighter (best performance + accuracy)
    url.append("&hl.tag.pre=").append(SolrUrlBuilder.urlEncode(FulltextResponseProperties.HIGHLIGHT_PRE.name));
    url.append("&hl.tag.post=").append(SolrUrlBuilder.urlEncode(FulltextResponseProperties.HIGHLIGHT_POST.name));
    // ====================================================


    url.append("&wt=json");
    url.append("&indent=true");

    // Final URL encoding for Solr special characters (solr uses this for certain operations)
    String finalUrl = SolrUrlBuilder.urlEncodeSolrSpecialCharacters(url.toString());

    return finalUrl;
  }


}
