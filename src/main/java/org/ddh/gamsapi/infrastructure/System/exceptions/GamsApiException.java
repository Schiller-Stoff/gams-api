package org.ddh.gamsapi.infrastructure.System.exceptions;

import org.springframework.http.HttpStatusCode;
import org.springframework.web.server.ResponseStatusException;

/**
 * General runtime exception for the GAMS API.
 * All other custom exceptions should extend this class.
 */
public class GamsApiException extends ResponseStatusException {

  public GamsApiException(HttpStatusCode status, String reason) {
    super(status, reason);
  }

  public GamsApiException(HttpStatusCode status, String reason, Throwable cause) {
    super(status, reason, cause);
  }
}
