package org.ddh.gamsapi.domain.DigitalObject.utils.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Exception representing: Requesting a digital object with given invalid date format.
 */
public class DigitalObjectInvalidDateFormatException extends DigitalObjectException {
  public DigitalObjectInvalidDateFormatException(String message) {
    super(HttpStatus.BAD_REQUEST, message);
  }

  public DigitalObjectInvalidDateFormatException(String message, Throwable cause) {
    super(HttpStatus.BAD_REQUEST, message, cause);
  }
}
