package org.zim.gamsapi.infrastructure.System.security.exceptions;

import org.springframework.http.HttpStatusCode;
import org.springframework.web.server.ResponseStatusException;

/**
 * Custom exceptions thrown via custom security processes.
 */
public class SecurityException extends ResponseStatusException {

  public SecurityException(HttpStatusCode status, String reason) {
    super(status, reason);
  }

}
