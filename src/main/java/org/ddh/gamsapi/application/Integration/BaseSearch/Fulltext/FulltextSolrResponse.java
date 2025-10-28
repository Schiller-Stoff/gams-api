package org.ddh.gamsapi.application.Integration.BaseSearch.Fulltext;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.Data;
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
   * Highlighting data from Solr.
   * Key: document ID
   * Value: Map of field name to list of highlighted snippets
   */
  private Map<String, Map<String, List<String>>> highlighting;

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
  private static Map<String, Map<String, List<String>>> parseHighlighting(JsonNode root) {
    Map<String, Map<String, List<String>>> result = new HashMap<>();

    JsonNode highlightingNode = root.path("highlighting");
    if (highlightingNode.isMissingNode() || !highlightingNode.isObject()) {
      log.debug("No highlighting data found in Solr response");
      return result;
    }

    Iterator<Map.Entry<String, JsonNode>> docIterator = highlightingNode.fields();
    while (docIterator.hasNext()) {
      Map.Entry<String, JsonNode> docEntry = docIterator.next();
      String docId = docEntry.getKey();
      JsonNode fieldsNode = docEntry.getValue();

      Map<String, List<String>> fieldHighlights = new HashMap<>();
      Iterator<Map.Entry<String, JsonNode>> fieldIterator = fieldsNode.fields();

      while (fieldIterator.hasNext()) {
        Map.Entry<String, JsonNode> fieldEntry = fieldIterator.next();
        String fieldName = fieldEntry.getKey();
        JsonNode snippetsNode = fieldEntry.getValue();

        List<String> snippets = new ArrayList<>();
        if (snippetsNode.isArray()) {
          for (JsonNode snippet : snippetsNode) {
            snippets.add(snippet.asText());
          }
        }

        if (!snippets.isEmpty()) {
          fieldHighlights.put(fieldName, snippets);
        }
      }

      if (!fieldHighlights.isEmpty()) {
        result.put(docId, fieldHighlights);
      }
    }

    log.debug("Parsed highlighting for {} documents", result.size());
    return result;
  }

}
