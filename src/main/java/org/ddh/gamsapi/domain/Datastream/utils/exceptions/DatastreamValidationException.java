package org.ddh.gamsapi.domain.Datastream.utils.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

public class DatastreamValidationException extends DatastreamException {
  public DatastreamValidationException(String reason) {
    super(HttpStatus.BAD_REQUEST, reason);
  }

  public DatastreamValidationException(String reason, Throwable cause) {
    super(HttpStatus.BAD_REQUEST, reason, cause);
  }
}
