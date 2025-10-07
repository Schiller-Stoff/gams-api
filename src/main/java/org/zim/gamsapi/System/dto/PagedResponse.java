package org.zim.gamsapi.System.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.domain.Page;
import java.util.List;

/**
 * Generic pagination response wrapper for REST API endpoints.
 * Provides consistent pagination metadata across all endpoints while
 * avoiding direct exposure of Spring Data's Page interface
 * Optimized for high-scale scenarios (millions of records).
 */
@Data
@Builder
@JacksonXmlRootElement(localName = "pagedResponse")
public class PagedResponse<T> {

  // Main content
  @JsonProperty("results")
  @JacksonXmlElementWrapper(localName = "results")
  @JacksonXmlProperty(localName = "result")
  private List<T> content;

  // Pagination metadata
  @JsonProperty("pagination")
  private PaginationInfo pagination;

  // Optional metadata for search/filtering contexts
  @JsonProperty("metadata")
  private ResponseMetadata metadata;

  @Data
  @Builder
  public static class PaginationInfo {
    @JsonProperty("page")
    private int page;

    @JsonProperty("size")
    private int size;

    @JsonProperty("totalElements")
    private long totalElements;

    @JsonProperty("totalPages")
    private int totalPages;

    @JsonProperty("hasNext")
    private boolean hasNext;

    @JsonProperty("hasPrevious")
    private boolean hasPrevious;

    @JsonProperty("isFirst")
    private boolean isFirst;

    @JsonProperty("isLast")
    private boolean isLast;


  }

  @Data
  @Builder
  public static class ResponseMetadata {
    @JsonProperty("searchTerm")
    private String searchTerm;

    @JsonProperty("filters")
    private Object filters;

    @JsonProperty("processingTimeMs")
    private Long processingTimeMs;

    @JsonProperty("resultSummary")
    private String resultSummary;

    // For future extensions like facets, aggregations, etc.
    @JsonProperty("facets")
    private Object facets;
  }

  /**
   * Factory method to create PagedResponse from Spring Data Page.
   *
   * @param page Spring Data Page object
   * @param <T> Type of content
   * @return PagedResponse with pagination metadata
   */
  public static <T> PagedResponse<T> from(Page<T> page) {
    return PagedResponse.<T>builder()
        .content(page.getContent())
        .pagination(PaginationInfo.builder()
            .page(page.getNumber())
            .size(page.getSize())
            .totalElements(page.getTotalElements())
            .totalPages(page.getTotalPages())
            .hasNext(page.hasNext())
            .hasPrevious(page.hasPrevious())
            .isFirst(page.isFirst())
            .isLast(page.isLast())
            .build())
        .build();
  }

  /**
   * Factory method with custom metadata.
   *
   * @param page Spring Data Page object
   * @param metadata Additional response metadata
   * @param <T> Type of content
   * @return PagedResponse with pagination and custom metadata
   */
  public static <T> PagedResponse<T> from(Page<T> page, ResponseMetadata metadata) {
    PagedResponse<T> response = from(page);
    response.setMetadata(metadata);
    return response;
  }

  /**
   * Factory method for search results with processing time.
   *
   * @param page Spring Data Page object
   * @param searchTerm Search term used
   * @param processingTimeMs Time taken to process request
   * @param <T> Type of content
   * @return PagedResponse with search metadata
   */
  public static <T> PagedResponse<T> fromSearch(Page<T> page, String searchTerm, Long processingTimeMs) {
    ResponseMetadata metadata = ResponseMetadata.builder()
        .searchTerm(searchTerm)
        .processingTimeMs(processingTimeMs)
        .resultSummary(String.format("Found %d results", page.getTotalElements()))
        .build();

    return from(page, metadata);
  }



}
