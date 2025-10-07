package org.zim.gamsapi.DigitalObject.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * A digital object might not contain itself as child object.
 */
public class DigitalObjectChildSelfReferenceException extends ResponseStatusException {

  public DigitalObjectChildSelfReferenceException(String reason) {
    super(HttpStatus.CONFLICT, reason);
  }

}
