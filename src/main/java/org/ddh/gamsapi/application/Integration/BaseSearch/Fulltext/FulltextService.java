package org.ddh.gamsapi.application.Integration.BaseSearch.Fulltext;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ddh.gamsapi.application.Integration.BaseSearch.solr.SolrClient;
import org.ddh.gamsapi.application.Integration.BaseSearch.solr.SolrGamsCores;
import org.ddh.gamsapi.infrastructure.System.dto.PagedResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;

import java.util.HashMap;
import java.util.List;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class FulltextService {

  private final SolrClient solrClient;

  // TODO think about what to return here
  public FulltextDigitalObjectResultDto searchDigitalObjectsByDublinCoreCriteria(
      String fulltextQuery,
      HashMap<String, List<String>> dublinCoreFilters,
      Set<String> projectAbbrs,
      Pageable pageable){

    var dcFiltersMultiValueMap = MultiValueMap.fromMultiValue(dublinCoreFilters);

    // 01. build solr query from dublinCoreFilters, projectAbbrs, searchMode, pageable
    var fulltextQueryParam = FulltextQueryBuilder.buildBaseSolrQuery(projectAbbrs, fulltextQuery);


    // 02. Build filter queries from dublinCoreFilters
    var filterQueries = FulltextQueryBuilder.buildSolrFilterQueries(dcFiltersMultiValueMap);

    // 03. build solr url
    var fulltextSolrUrl =  FulltextQueryBuilder.buildSolrUrl(
        SolrGamsCores.GAMS_CORE.value, // solr core
        fulltextQueryParam, // q parameter
        filterQueries, // filter-queries for combined dc-search
        pageable // pageable
    );

    // 04. execute solr query via solrClient
    var fullTextSolrResponse =  solrClient.get(fulltextSolrUrl);
    var fulltextResponseParsed = FulltextSolrResponse.from(fullTextSolrResponse);

    // 05: Get baseline total count for these projects (unfiltered)
    int projectDocumentsCount = solrClient.countProjectDocuments(
        SolrGamsCores.GAMS_CORE.value, projectAbbrs);

    // 06. parse solr response and build PagedResponse<DigitalObjectSearchResultDTO>
    return FulltextDigitalObjectResultDto.from(
        fulltextResponseParsed,
        dcFiltersMultiValueMap,
        projectDocumentsCount,
        pageable
    );

  }

}
