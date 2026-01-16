package org.ddh.gamsapi.application.Integration.CustomSearch;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.ddh.gamsapi.application.Integration.Common.exceptions.IntegrationDataProcessingException;
import org.ddh.gamsapi.application.Integration.Common.utils.solr.SolrDocument;
import org.ddh.gamsapi.infrastructure.System.dto.PagedResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Builder
@Data
@Slf4j
public class CustomSearchResponseDto {

  @Schema(description = "Paginated search results with complete metadata")
  private PagedResponse<SolrDocument> results;

  private Map<String, List<String>> selectedFilter;

  // TODO do i need this?
  private long totalUnfilteredCount;

  /**
   * TODO jdoc
   * TODO test
   * @param solrResponse
   * @param pageable // TODO rename - this is the current client's pagination info
   * @return
   * @throws IntegrationDataProcessingException
   */
  public static CustomSearchResponseDto from(String solrResponse, Pageable pageable) throws IntegrationDataProcessingException {
    ObjectMapper objectMapper = new ObjectMapper();
    try {
      // Parse response metadata
      JsonNode root = objectMapper.readTree(solrResponse);
      JsonNode responseNode = root.path("response"); //TODO hardcoded
      long numFound = responseNode.path("numFound").asLong(); // TODO hardcoded
      long start = responseNode.path("start").asLong(); // TODO hardcoded
      // Parse documents
      List<SolrDocument> documents = new ArrayList<>();
      JsonNode docsNode = responseNode.path("docs");
      if (docsNode.isArray()) {
        for (JsonNode docNode : docsNode) {
          var doc = objectMapper.treeToValue(docNode, SolrDocument.class);
          documents.add(doc);
        }
      }

      Page<SolrDocument> page = new PageImpl<>(
          documents,                      // Content for this page
          pageable,                           // Pageable (contains page number, size, sort)
          numFound                          // TODO is this correct?
      );

      // Step 3: Convert to standard PagedResponse wrapper
      PagedResponse<SolrDocument> pagedResults = PagedResponse.from(page);

      return CustomSearchResponseDto.builder()
          .results(pagedResults)
          .totalUnfilteredCount(numFound)
          .build();

    } catch (IOException e) {
      String msg = String.format("Failed to parse solr response for custom search. Original error: %s", e.getMessage());
      log.error(msg);
      throw new IntegrationDataProcessingException(msg);
    }
  }

}
