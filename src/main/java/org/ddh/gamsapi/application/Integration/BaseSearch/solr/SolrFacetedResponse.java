package org.ddh.gamsapi.application.Integration.BaseSearch.solr;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.ddh.gamsapi.application.Integration.BaseSearch.BaseSearchProperties;
import org.ddh.gamsapi.application.Integration.Common.exceptions.IntegrationDataProcessingException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Helper class to hold parsed Solr faceted response.
 */
@Data
@Builder
@Slf4j
public class SolrFacetedResponse {
  private List<SolrDocument> documents;
  private Map<String, List<SolrFacetValue>> facets;
  private long numFound;
  private long start;


  /**
   * Creates class instance from a solrResponse document for facet querying.
   * Expects the solr response in JSON format as String.
   * @param solrResponse Solr response as String
   * @return SolrFacetedResponse instance
   */
  public static SolrFacetedResponse from(String solrResponse){

    // TODO use propper jackson workflow to parse solr repsonse? (is this even possible?)

    try {
      var OBJECT_MAPPER = new com.fasterxml.jackson.databind.ObjectMapper();
      JsonNode root = OBJECT_MAPPER.readTree(solrResponse);

      // Parse response metadata
      JsonNode responseNode = root.path("response");
      long numFound = responseNode.path("numFound").asLong();
      long start = responseNode.path("start").asLong();

      // Parse documents
      List<SolrDocument> documents = new ArrayList<>();
      JsonNode docsNode = responseNode.path("docs");
      if (docsNode.isArray()) {
        for (JsonNode docNode : docsNode) {
          var doc = OBJECT_MAPPER.treeToValue(docNode, SolrDocument.class);
          documents.add(doc);
        }
      }

      // Parse facets
      Map<String, List<SolrFacetValue>> facets = new HashMap<>();
      JsonNode facetFields = root.path("facet_counts").path("facet_fields");

      if (facetFields.isObject()) {
        facetFields.fields().forEachRemaining(entry -> {
          String fieldName = entry.getKey();
          JsonNode facetArray = entry.getValue();

          List<SolrFacetValue> solrFacetValues = new ArrayList<>();

          // Solr returns facets as alternating value/count array
          // Format: ["value1", count1, "value2", count2, ...]
          if (facetArray.isArray()) {
            for (int i = 0; i < facetArray.size(); i += 2) {
              if (i + 1 < facetArray.size()) {
                String value = facetArray.get(i).asText();
                long count = facetArray.get(i + 1).asLong();

                if (count > 0) { // Only include non-zero counts
                  solrFacetValues.add(SolrFacetValue.builder()
                      .value(value)
                      .count(count)
                      .selected(false) // Will be set later based on selectedFacets
                      .build());
                }
              }
            }
          }

          // TODO this sorting seems to be very weird here? Shouldn't be necessary with proper solr query params?

          // Sort facet values by count (descending) then by value (ascending)
          solrFacetValues.sort((a, b) -> {
            int countCompare = Long.compare(b.getCount(), a.getCount());
            return countCompare != 0 ? countCompare : a.getValue().compareTo(b.getValue());
          });

          // Keep field name as-is (already in "dc.fieldname" format)
          facets.put(fieldName, solrFacetValues);
        });
      }

      return SolrFacetedResponse.builder()
          .documents(documents)
          .facets(facets)
          .numFound(numFound)
          .start(start)
          .build();

    } catch (Exception e) {
      String msg = String.format("Failed to parse Solr faceted response. Cause: %s", e.getMessage());
      log.error(msg, e);
      throw new IntegrationDataProcessingException(msg);
    }

  }
}
