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
   * Performs faceted Dublin Core search in Solr.
   * Uses the updated schema with "dc.fieldname" format where all language variants
   * are stored in a single multi-valued field (e.g., dc.title = ["english title", "german title"])
   *
   * @param projectAbbrs Set of project abbreviations to filter by
   * @param selectedFacets MultiValueMap of selected Dublin Core facets (field -> values)
   * @param pageable Pagination information
   * @return FacetSearchResponse containing results, facets, and metadata
   */
  public FacetResponseDTO facetSearch(
      Set<String> projectAbbrs,
      MultiValueMap<String, String> selectedFacets,
      Pageable pageable) {

    long startTime = System.currentTimeMillis();

    log.debug("Solr faceted search: projects={}, filters={}, page={}",
        projectAbbrs, selectedFacets, pageable);

    // Validate inputs
    if (projectAbbrs == null || projectAbbrs.isEmpty()) {
      throw new IntegrationDataProcessingException("Project abbreviations must not be empty");
    }

    // STEP 1: Build Solr query with filters (just the value for the q parameter)
    String solrQuery = FacetQueryBuilder.buildSolrFacetQuery(projectAbbrs, selectedFacets);

    // STEP 2: Define default facet fields (Dublin Core standard fields with "dc." prefix)
    Set<String> facetFields = getDefaultDublinCoreFacetFields();

    // STEP 3: Execute Solr search with faceting logic / pagination etc.
    String solrFacetUrl = FacetQueryBuilder.buildSolrFacetUrl(
        SolrGamsCores.GAMS_CORE.value,
        solrQuery,
        facetFields,
        pageable
    );

    String solrResponse = solrClient.get(solrFacetUrl);

    // STEP 4: Parse Solr response
    SolrFacetedResponse parsedResponse = SolrFacetedResponse.from(solrResponse);

    long totalTime = System.currentTimeMillis() - startTime;

    log.info("Solr faceted search completed in {}ms - found {} results with {} facet fields",
        totalTime, parsedResponse.getNumFound(), facetFields.size());


    // STEP 5: Transform to response from our API

    return FacetResponseDTO.from(
        parsedResponse, selectedFacets
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
