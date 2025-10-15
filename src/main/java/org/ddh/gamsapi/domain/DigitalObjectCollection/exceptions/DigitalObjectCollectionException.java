package org.ddh.gamsapi.domain.DigitalObjectCollection.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class DigitalObjectCollectionException extends ResponseStatusException {
  public DigitalObjectCollectionException(HttpStatus status, String reason) {
    super(status, reason);
  }
}
