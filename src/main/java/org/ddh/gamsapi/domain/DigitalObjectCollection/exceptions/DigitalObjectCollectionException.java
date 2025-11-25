package org.ddh.gamsapi.domain.DigitalObjectCollection.exceptions;

import org.ddh.gamsapi.infrastructure.System.exceptions.GamsApiException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class DigitalObjectCollectionException extends GamsApiException {
  public DigitalObjectCollectionException(HttpStatus status, String reason) {
    super(status, reason);
  }

  public DigitalObjectCollectionException(HttpStatus status, String reason, Throwable cause) {
    super(status, reason, cause);
  }
}
