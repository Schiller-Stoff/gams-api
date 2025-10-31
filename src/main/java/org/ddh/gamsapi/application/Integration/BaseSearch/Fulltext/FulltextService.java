package org.ddh.gamsapi.application.Integration.BaseSearch.Fulltext;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ddh.gamsapi.application.Integration.BaseSearch.DublinCoreSearchMode;
import org.ddh.gamsapi.application.Integration.BaseSearch.solr.SolrClient;
import org.ddh.gamsapi.application.Integration.BaseSearch.solr.SolrGamsCores;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;

import java.util.HashMap;
import java.util.List;
import java.util.Set;

/**
 * Service for performing fulltext searches on Dublin Core metadata in Solr.
 *
 * <p>Supports two search modes for Dublin Core filters:</p>
 * <ul>
 *   <li><b>PHRASE:</b> Exact phrase matching (default)</li>
 *   <li><b>SUBSTRING:</b> Tokenized substring matching</li>
 * </ul>
 *
 * <p><b>Example Usage:</b></p>
 * <pre>
 * // Exact match: Only finds documents with dc.subject = "Tag"
 * searchDigitalObjectsByDublinCoreCriteria(
 *     "medieval",
 *     Map.of("dc.subject", List.of("Tag")),
 *     Set.of("project1"),
 *     DublinCoreSearchMode.PHRASE,
 *     PageRequest.of(0, 20)
 * );
 *
 * // Substring match: Finds "Tag", "Tagsatzung", "Tags", etc.
 * searchDigitalObjectsByDublinCoreCriteria(
 *     "medieval",
 *     Map.of("dc.subject", List.of("Tag")),
 *     Set.of("project1"),
 *     DublinCoreSearchMode.SUBSTRING,
 *     PageRequest.of(0, 20)
 * );
 * </pre>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class FulltextService {

  private final SolrClient solrClient;

  /**
   * Performs fulltext search with Dublin Core filters.
   *
   * <p><b>Search Logic:</b></p>
   * <ol>
   *   <li>Base query: Matches {@code objectFulltext} AND {@code objectProjectAbbr}</li>
   *   <li>DC Filters: Applied as filter queries (fq) based on search mode</li>
   *   <li>Results: Paginated with highlighting on matched terms</li>
   * </ol>
   *
   * <p><b>Mode Behavior:</b></p>
   * <table border="1">
   *   <tr>
   *     <th>Mode</th>
   *     <th>Filter Example</th>
   *     <th>Solr Field</th>
   *     <th>Matches</th>
   *   </tr>
   *   <tr>
   *     <td>PHRASE</td>
   *     <td>dc.subject=Tag</td>
   *     <td>dc.subject</td>
   *     <td>Only "Tag"</td>
   *   </tr>
   *   <tr>
   *     <td>SUBSTRING</td>
   *     <td>dc.subject=Tag</td>
   *     <td>dc.subject_txt</td>
   *     <td>"Tag", "Tagsatzung", "Tags"</td>
   *   </tr>
   * </table>
   *
   * @param fulltextQuery Main fulltext search query (searches objectFulltext field)
   * @param dublinCoreFilters Map of Dublin Core field filters (field -> values)
   * @param projectAbbrs Set of project abbreviations to limit search scope
   * @param dcSearchMode Search mode for Dublin Core filters (PHRASE or SUBSTRING)
   * @param pageable Pagination and sorting information
   * @return Paginated search results with highlighting and metadata
   */
  public FulltextDigitalObjectResultDto search(
      String fulltextQuery,
      HashMap<String, List<String>> dublinCoreFilters,
      Set<String> projectAbbrs,
      Pageable pageable
  ) {
    long startTime = System.currentTimeMillis();

    log.debug("Fulltext search: query='{}', DC filters={}, projects={}, page={}, size={}",
        fulltextQuery, dublinCoreFilters, projectAbbrs,
        pageable.getPageNumber(), pageable.getPageSize());

    var dcFiltersMultiValueMap = MultiValueMap.fromMultiValue(dublinCoreFilters);

    // 01. Build base Solr query from project + fulltext
    var fulltextQueryParam = FulltextQueryBuilder.buildBaseSolrQuery(
        projectAbbrs,
        fulltextQuery
    );

    // 02. Build filter queries from Dublin Core filters WITH SEARCH MODE
    var filterQueries = FulltextQueryBuilder.buildSolrFilterQueries(
        dcFiltersMultiValueMap
    );

    // 03. Build complete Solr URL
    var fulltextSolrUrl = FulltextQueryBuilder.buildSolrUrl(
        SolrGamsCores.GAMS_CORE.value,
        fulltextQueryParam,
        filterQueries,
        pageable
    );

    log.debug("Constructed Solr fulltext URL: {}", fulltextSolrUrl);

    // 04. Execute Solr query
    var fullTextSolrResponse = solrClient.get(fulltextSolrUrl);
    var fulltextResponseParsed = FulltextSolrResponse.from(fullTextSolrResponse);

    // 05. Get baseline total count for these projects (unfiltered)
    int projectDocumentsCount = solrClient.countProjectDocuments(
        SolrGamsCores.GAMS_CORE.value, projectAbbrs
    );

    long totalTime = System.currentTimeMillis() - startTime;
    log.info("Fulltext search completed in {}ms - found {} results",
        totalTime, fulltextResponseParsed.getNumFound());

    // 06. Parse Solr response and build result DTO
    return FulltextDigitalObjectResultDto.from(
        fulltextResponseParsed,
        dcFiltersMultiValueMap,
        projectDocumentsCount,
        pageable
    );
  }
}