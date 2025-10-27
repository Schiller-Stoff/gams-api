package org.ddh.gamsapi.application.Integration.BaseSearch.Fulltext;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.ddh.gamsapi.application.Integration.BaseSearch.solr.SolrDocument;
import org.ddh.gamsapi.application.Integration.Common.exceptions.IntegrationDataProcessingException;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Builder
@Data
public class FulltextSolrResponse {

  private List<SolrDocument> documents;
  private long numFound;
  private long start;

  /**
   * Creates class instance from a solrResponse document for facet querying.
   * Expects the solr response in JSON format as String.
   * @param solrResponse Solr response as String
   * @return SolrFacetedResponse instance
   */
  public static FulltextSolrResponse from(String solrResponse){

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

      return FulltextSolrResponse.builder()
          .documents(documents)
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
