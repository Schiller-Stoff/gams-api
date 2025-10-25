package org.ddh.gamsapi.application.Integration.BaseSearch.Facet;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ddh.gamsapi.application.Integration.BaseSearch.solr.SolrClient;
import org.ddh.gamsapi.application.Integration.BaseSearch.solr.SolrFacetedResponse;
import org.ddh.gamsapi.application.Integration.BaseSearch.solr.SolrGamsCores;
import org.ddh.gamsapi.application.Integration.Common.exceptions.IntegrationDataProcessingException;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;

import java.util.Set;

/**
 * Service for performing faceted searches on Dublin Core metadata in Solr.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class FacetService {


  private final SolrClient solrClient;

  /**
   * Performs faceted Dublin Core search in Solr with drill-down support.
   *
   * Uses the updated schema with "dc.fieldname" format where all language variants
   * are stored in a single multi-valued field (e.g., dc.title = ["english title", "german title"])
   *
   * Implements drill-down using Solr's tag/exclusion feature:
   * - Base query contains only project + fulltext
   * - Facet filters are in separate fq (filter query) parameters with tags
   * - Each facet field excludes its own tag when counting
   *
   * @param projectAbbrs Set of project abbreviations to filter by
   * @param fulltextQuery Fulltext search query - if empty finds everything
   * @param selectedFacets MultiValueMap of selected Dublin Core facets (field -> values)
   * @param pageable Pagination information
   * @return FacetSearchResponse containing results, facets, and metadata
   */
  public FacetResponseDTO facetSearch(
      Set<String> projectAbbrs,
      String fulltextQuery,
      MultiValueMap<String, String> selectedFacets,
      Pageable pageable) {

    long startTime = System.currentTimeMillis();

    log.debug("Solr faceted search with drill-down: projects={}, fulltext={}, filters={}, page={}",
        projectAbbrs, fulltextQuery, selectedFacets, pageable);

    // Validate inputs
    if (projectAbbrs == null || projectAbbrs.isEmpty()) {
      throw new IntegrationDataProcessingException("Project abbreviations must not be empty");
    }

    // Define Dublin Core facet fields to use
    Set<String> facetFields = getDefaultDublinCoreFacetFields();

    // STEP 1: Build SOLR url with drill-down support
    String solrFacetUrl =  FacetQueryBuilder.buildSolrFacetDrilldownUrl(
        SolrGamsCores.GAMS_CORE.value,
        projectAbbrs,
        fulltextQuery,
        selectedFacets,
        facetFields,
        pageable
    );

    log.trace("Constructed Solr faceted search URL: {}", solrFacetUrl);

    // STEP 2: Execute Solr query
    String solrResponse = solrClient.get(solrFacetUrl);

    // STEP 3: Parse Solr response
    SolrFacetedResponse parsedResponse = SolrFacetedResponse.from(solrResponse);

    // STEP 4: Get baseline total count for these projects
    int projectDocumentsCount = solrClient.countProjectDocuments(
        SolrGamsCores.GAMS_CORE.value, projectAbbrs);

    long totalTime = System.currentTimeMillis() - startTime;

    log.info("Solr faceted search with drill-down completed in {}ms - found {} filtered results out of {} total with {} facet fields",
        totalTime, parsedResponse.getNumFound(), projectDocumentsCount, facetFields.size());

    // STEP 5: Transform to response DTO with selected facets marked
    return FacetResponseDTO.from(
        parsedResponse,
        selectedFacets,
        projectDocumentsCount
    );
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


}
