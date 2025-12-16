package org.ddh.gamsapi.domain.DigitalObject.utils.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Digital object not found representing status 404
 */
public class DigitalObjectNotFoundException extends DigitalObjectException {

  public DigitalObjectNotFoundException(String reason) {
    super(HttpStatus.NOT_FOUND, reason);
  }

  public DigitalObjectNotFoundException(String reason, Throwable cause) {
    super(HttpStatus.NOT_FOUND, reason, cause);
  }
}
