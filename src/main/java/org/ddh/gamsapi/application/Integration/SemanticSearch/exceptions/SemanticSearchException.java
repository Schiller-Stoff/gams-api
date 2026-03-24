package org.ddh.gamsapi.application.Integration.SemanticSearch.exceptions;

import org.ddh.gamsapi.application.Integration.Common.exceptions.IntegrationException;
import org.springframework.http.HttpStatus;

/**
 * General exception for Semantic Search integration errors.
 */
public class SemanticSearchException extends IntegrationException {

  public SemanticSearchException(HttpStatus status, String reason) {
    super(status, reason);
  }

  public SemanticSearchException(HttpStatus status, String reason, Throwable cause) {
    super(status, reason, cause);
  }
}
