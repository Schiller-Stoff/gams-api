package org.ddh.gamsapi.application.Integration.SemanticSearch.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Indicates unexpected IO problems when using the semantic search service
 */
public class SemanticSearchIOException extends SemanticSearchException {
  public SemanticSearchIOException(String message) {
    super(HttpStatus.INTERNAL_SERVER_ERROR, message);
  }

  public SemanticSearchIOException(String message, Throwable cause) {
    super(HttpStatus.INTERNAL_SERVER_ERROR, message, cause);
  }
}
