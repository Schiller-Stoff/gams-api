package org.zim.gamsapi.DigitalObject.exceptions;

import org.springframework.http.HttpStatus;
import org.zim.gamsapi.DigitalObject.exceptions.DigitalObjectException;

/**
 * Digital object not found representing status 404
 */
public class DigitalObjectNotFoundException extends DigitalObjectException {

  public DigitalObjectNotFoundException(String reason) {
    super(HttpStatus.NOT_FOUND, reason);
  }
}
