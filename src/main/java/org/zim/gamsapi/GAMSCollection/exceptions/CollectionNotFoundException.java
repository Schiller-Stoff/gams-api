package org.zim.gamsapi.GAMSCollection.exceptions;

import org.springframework.http.HttpStatus;

public class CollectionNotFoundException extends CollectionException {
  public CollectionNotFoundException(String reason) {
    super(HttpStatus.NOT_FOUND, reason);
  }
}
