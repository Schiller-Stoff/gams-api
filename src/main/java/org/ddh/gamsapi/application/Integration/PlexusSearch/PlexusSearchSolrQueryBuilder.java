package org.ddh.gamsapi.application.Integration.PlexusSearch;

import org.ddh.gamsapi.application.Integration.PlexusSearch.dto.PlexusSearchQueryRequestDto;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class PlexusSearchSolrQueryBuilder {

  /**
   * Builds Solr query URL from request parameters.
   */
  public static String buildSolrQueryUrl(
      String coreName,
      PlexusSearchQueryRequestDto request,
      List<String> filterQueries
  ) {
    // TODO move logic to own class PlexusSearchSolrQueryBuilder.java?

    StringBuilder url = new StringBuilder();

    // TODO use gamsDockerDns or config value
    url.append("/solr/").append(coreName).append("/select");
    url.append("?q=").append(encodeQueryParam(request.getQuery()));
    url.append("&rows=").append(request.getRows());
    url.append("&start=").append(request.getStart());

    // Add filter queries (includes mandatory project filter)
    for (String fq : filterQueries) {
      url.append("&fq=").append(encodeQueryParam(fq));
    }

    // Add sort
    if (request.getSort() != null && !request.getSort().isEmpty()) {
      url.append("&sort=").append(encodeQueryParam(request.getSort()));
    }

    // Add highlighting
    if (Boolean.TRUE.equals(request.getHighlight())) {
      url.append("&hl=true");
      if (request.getHighlightFields() != null && !request.getHighlightFields().isEmpty()) {
        url.append("&hl.fl=").append(String.join(",", request.getHighlightFields()));
      }
    }

    // Add faceting
    if (request.getFacetFields() != null && !request.getFacetFields().isEmpty()) {
      url.append("&facet=true");
      url.append("&facet.field=").append(String.join("&facet.field=", request.getFacetFields()));
      url.append("&facet.limit=").append(request.getFacetLimit());
    }

    // Add cursor mark if provided
    if (request.getCursorMark() != null && !request.getCursorMark().isEmpty()) {
      url.append("&cursorMark=").append(encodeQueryParam(request.getCursorMark()));
      // Cursor pagination requires a sort with unique field
      if (request.getSort() == null || request.getSort().isEmpty()) {
        url.append("&sort=").append(PlexusSearchProperties.ENTITY_ID.name).append(" asc");
      }
    }

    return url.toString();
  }

  /**
   * URL-encodes query parameter values.
   */
  public static String encodeQueryParam(String value) {
    try {
      return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8);
    } catch (Exception e) {
      return value; // Fallback
    }
  }


}
