package org.zim.gamsapi.domain.DigitalObjectCollection.exceptions;

import org.springframework.http.HttpStatus;

public class DigitalObjectCollectionNotFoundException extends DigitalObjectCollectionException {
  public DigitalObjectCollectionNotFoundException(String reason) {
    super(HttpStatus.NOT_FOUND, reason);
  }
}
