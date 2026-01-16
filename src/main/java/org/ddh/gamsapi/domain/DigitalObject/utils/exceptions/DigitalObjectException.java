package org.ddh.gamsapi.domain.DigitalObject.utils.exceptions;

import org.ddh.gamsapi.infrastructure.System.exceptions.GamsApiException;
import org.springframework.http.HttpStatusCode;

/**
 * Exceptions related to digital objects.
 */
public class DigitalObjectException extends GamsApiException {
  public DigitalObjectException(HttpStatusCode status, String reason) {
    super(status, reason);
  }

  public DigitalObjectException(HttpStatusCode status, String reason, Throwable cause) {
    super(status, reason, cause);
  }
}
