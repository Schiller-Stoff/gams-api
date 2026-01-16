package org.ddh.gamsapi.domain.DigitalObject.utils.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Exception representing a digital object without a mainResource datastream defined
 * but needed in given situation.
 */
public class DigitalObjectNoMainResourceDatastreamDefinedException extends DigitalObjectException {
  public DigitalObjectNoMainResourceDatastreamDefinedException(String message) {
    super(HttpStatus.CONFLICT, message);
  }

  public DigitalObjectNoMainResourceDatastreamDefinedException(String message, Throwable cause) {
    super(HttpStatus.CONFLICT, message, cause);
  }
}
