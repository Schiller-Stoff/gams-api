package org.zim.gamsapi.DigitalObject.exceptions;

import org.springframework.http.HttpStatus;


public class DigitalObjectConversionException extends DigitalObjectException {
  public DigitalObjectConversionException(String reason) {
    super(HttpStatus.INTERNAL_SERVER_ERROR, reason);
  }
}
