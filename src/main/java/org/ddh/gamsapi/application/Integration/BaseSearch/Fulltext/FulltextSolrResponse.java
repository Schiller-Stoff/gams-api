package org.ddh.gamsapi.application.Integration.BaseSearch.Fulltext;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ddh.gamsapi.application.Integration.BaseSearch.solr.SolrDocument;
import org.ddh.gamsapi.application.Integration.Common.exceptions.IntegrationDataProcessingException;

import java.util.*;

@Slf4j
@Builder
@Data
public class FulltextSolrResponse {

  private List<SolrDocument> documents;
  private long numFound;
  private long start;

  /**
   * Highlighting parsed from Solr response.
   */
  private Map<String, List<String>> highlighting;

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

      var highlightingData = parseHighlighting(root);

      return FulltextSolrResponse.builder()
          .documents(documents)
          .numFound(numFound)
          .start(start)
          .highlighting(highlightingData)
          .build();

    } catch (Exception e) {
      String msg = String.format("Failed to parse Solr faceted response. Cause: %s", e.getMessage());
      log.error(msg, e);
      throw new IntegrationDataProcessingException(msg);
    }

  }

  /**
   * Extracts highlighting data from Solr response.
   * Solr highlighting format:
   * {
   *   "highlighting": {
   *     "docId1": {
   *       "fieldName": ["<em>highlighted</em> text", "another <em>match</em>"]
   *     }
   *   }
   * }
   */
  private static Map<String, List<String>> parseHighlighting(JsonNode root) {

    Map<String, List<String>> simplifiedHighlights = new HashMap<>();

    JsonNode highlightingNode = root.path("highlighting");
    if (highlightingNode.isMissingNode() || !highlightingNode.isObject()) {
      log.debug("No highlighting data found in Solr response");
      return simplifiedHighlights;
    }

    Iterator<Map.Entry<String, JsonNode>> docIterator = highlightingNode.fields();
    while (docIterator.hasNext()) {
      Map.Entry<String, JsonNode> docEntry = docIterator.next();
      String docId = docEntry.getKey();
      List<String> snippets = new ArrayList<>();
      JsonNode fieldsNode = docEntry.getValue();

      Iterator<Map.Entry<String, JsonNode>> fieldIterator = fieldsNode.fields();

      while (fieldIterator.hasNext()) {
        Map.Entry<String, JsonNode> fieldEntry = fieldIterator.next();
        JsonNode snippetsNode = fieldEntry.getValue();

        // this is always an array of highlighted snippets!
        if (snippetsNode.isArray()) {
          for (JsonNode snippet : snippetsNode) {
            snippets.add(snippet.asText());
          }
        } else {
          log.warn("Expected array of snippets for field '{}', but found: {}", fieldEntry.getKey(), snippetsNode);
        }
      }

      simplifiedHighlights.put(docId, snippets);
    }

    log.debug("Parsed highlighting for {} documents", simplifiedHighlights.size());
    return simplifiedHighlights;
  }

  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @Data
  public static class FulltextSolrHighlighting {

    /**
     * Field-to-snippets mapping.
     * Key: field name (e.g., "objectFulltext", "dc.title")
     * Value: List of highlighted text fragments
     */
    private Map<String, List<String>> snippets;


    /**
     * Total number of highlighted fragments across all fields
     */
    private int totalFragments;

  }

}
