package org.ddh.gamsapi.application.Integration.SemanticSearch.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Represents error states where inside a datastream content no triples could be extracted.
 * Most possible due to empty file or malformed data.
 */
public class SemanticSearchNoTriplesExtractableException extends SemanticSearchException {
  public SemanticSearchNoTriplesExtractableException(String message) {
    super(HttpStatus.BAD_REQUEST, message);
  }

  public SemanticSearchNoTriplesExtractableException(String message, Throwable cause) {
    super(HttpStatus.BAD_REQUEST, message, cause);
  }
}
