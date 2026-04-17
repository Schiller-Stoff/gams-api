package org.ddh.gamsapi.domain.DigitalObject.utils.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Represents error states where the validation of a digital object failed.
 */
public class DigitalObjectValidationException extends DigitalObjectException {

  public DigitalObjectValidationException(String reason) {
    super(HttpStatus.BAD_REQUEST, reason);
  }

  public DigitalObjectValidationException(String reason, Throwable cause) {
    super(HttpStatus.BAD_REQUEST, reason, cause);
  }
}
