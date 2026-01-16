package org.ddh.gamsapi.domain.Datastream.utils.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Represents error states  when multiple datastreams were found BUT only a specific one
 * was required and no further information was provided to distinguish between them.
 */
public class DatastreamAmbiguousMatchException extends DatastreamException {
  public DatastreamAmbiguousMatchException(String message) {
    super(HttpStatus.CONFLICT, message);
  }

  public DatastreamAmbiguousMatchException(String message, Throwable cause) {
    super(HttpStatus.CONFLICT, message, cause);
  }
}
