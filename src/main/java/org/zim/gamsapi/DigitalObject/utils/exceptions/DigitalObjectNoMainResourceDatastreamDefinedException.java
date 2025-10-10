package org.zim.gamsapi.DigitalObject.utils.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Exception representing a digital object without a mainResource datastream defined
 * but needed in given situation.
 */
public class DigitalObjectNoMainResourceDatastreamDefinedException extends DigitalObjectException {
  public DigitalObjectNoMainResourceDatastreamDefinedException(String message) {
    super(HttpStatus.INTERNAL_SERVER_ERROR, message);
  }
}
