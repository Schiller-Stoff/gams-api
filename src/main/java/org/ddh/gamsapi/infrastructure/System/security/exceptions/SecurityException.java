package org.ddh.gamsapi.infrastructure.System.security.exceptions;

import org.ddh.gamsapi.infrastructure.System.exceptions.GamsApiException;
import org.springframework.http.HttpStatusCode;

/**
 * Custom exceptions thrown via custom security processes.
 */
public class SecurityException extends GamsApiException {

  public SecurityException(HttpStatusCode status, String reason) {
    super(status, reason);
  }

  public SecurityException(HttpStatusCode status, String reason, Throwable cause) {
    super(status, reason, cause);
  }
}
