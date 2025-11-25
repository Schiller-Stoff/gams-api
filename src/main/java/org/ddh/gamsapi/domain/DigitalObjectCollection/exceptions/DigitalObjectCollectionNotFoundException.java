package org.ddh.gamsapi.domain.DigitalObjectCollection.exceptions;

import org.springframework.http.HttpStatus;

public class DigitalObjectCollectionNotFoundException extends DigitalObjectCollectionException {
  public DigitalObjectCollectionNotFoundException(String reason) {
    super(HttpStatus.NOT_FOUND, reason);
  }

  public DigitalObjectCollectionNotFoundException(String reason, Throwable cause) {
    super(HttpStatus.NOT_FOUND, reason, cause);
  }
}
