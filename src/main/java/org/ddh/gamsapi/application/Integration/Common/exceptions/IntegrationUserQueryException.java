package org.ddh.gamsapi.application.Integration.Common.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Exception for invalid user queries in/for integration operations.
 * E.g. invalid search queries via url.
 */
public class IntegrationUserQueryException extends IntegrationException {
  public IntegrationUserQueryException(String reason) {
    super(HttpStatus.BAD_REQUEST, reason);
  }

  public IntegrationUserQueryException(String reason, Throwable cause) {
    super(HttpStatus.BAD_REQUEST, reason, cause);
  }
}
