package org.zim.gamsapi.domain.GAMSCollection.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class CollectionException extends ResponseStatusException {
  public CollectionException(HttpStatus status, String reason) {
    super(status, reason);
  }
}
