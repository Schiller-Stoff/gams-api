package org.ddh.gamsapi.domain.Datastream.utils.exceptions;

import org.springframework.http.HttpStatus;


/**
 * Represents error state where the datastream id hashing failed.
 */
public class DatastreamIdHashingException extends DatastreamException {

  public DatastreamIdHashingException(String reason) {
    super(HttpStatus.INTERNAL_SERVER_ERROR, reason);
  }

  public DatastreamIdHashingException(String reason, Throwable cause) {
    super(HttpStatus.INTERNAL_SERVER_ERROR, reason, cause);
  }

}
