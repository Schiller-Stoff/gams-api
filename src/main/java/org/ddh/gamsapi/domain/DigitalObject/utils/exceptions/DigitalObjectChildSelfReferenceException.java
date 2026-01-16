package org.ddh.gamsapi.domain.DigitalObject.utils.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * A digital object might not contain itself as child object.
 */
public class DigitalObjectChildSelfReferenceException extends DigitalObjectException {

  public DigitalObjectChildSelfReferenceException(String reason) {
    super(HttpStatus.CONFLICT, reason);
  }

  public DigitalObjectChildSelfReferenceException(String reason, Throwable cause) {
    super(HttpStatus.CONFLICT, reason, cause);
  }

}
