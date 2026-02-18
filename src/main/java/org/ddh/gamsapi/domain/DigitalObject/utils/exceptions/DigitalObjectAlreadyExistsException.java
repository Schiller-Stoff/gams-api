package org.ddh.gamsapi.domain.DigitalObject.utils.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Error state where a digital object already exists.
 */
public class DigitalObjectAlreadyExistsException extends DigitalObjectException {

  public DigitalObjectAlreadyExistsException(String reason){
    super(HttpStatus.CONFLICT, reason);
  }

  public DigitalObjectAlreadyExistsException(String reason, Throwable cause){
    super(HttpStatus.CONFLICT, reason, cause);
  }

}
