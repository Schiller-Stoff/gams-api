package org.ddh.gamsapi.application.Integration.BaseSearch.solr;

import lombok.Builder;
import lombok.Data;
import org.ddh.gamsapi.application.Integration.BaseSearch.BaseSearch;

import java.util.List;
import java.util.Map;

/**
 * Helper class to hold parsed Solr faceted response.
 */
@Data
@Builder
public class SolrFacetedResponse {
  // TODO rethink usage of BaseSearch class here
  private List<BaseSearch> documents;
  private Map<String, List<FacetValue>> facets;
  private long numFound;
  private long start;
  private long totalCount;
}
