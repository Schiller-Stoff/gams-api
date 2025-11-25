package org.ddh.gamsapi.domain.DigitalObject.utils.exceptions;

import org.springframework.http.HttpStatusCode;
import org.springframework.web.server.ResponseStatusException;

/**
 * Exceptions related to digital objects.
 */
public class DigitalObjectException extends ResponseStatusException {
  public DigitalObjectException(HttpStatusCode status, String reason) {
    super(status, reason);
  }

  public DigitalObjectException(HttpStatusCode status, String reason, Throwable cause) {
    super(status, reason, cause);
  }
}
