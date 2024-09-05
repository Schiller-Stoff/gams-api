package org.zim.gamsapi.Datastream.exceptions;

import org.springframework.http.HttpStatus;


/**
 * Represents error state where the datastream id hashing failed.
 */
public class DatastreamIdHashingException extends DatastreamException {

  public DatastreamIdHashingException(String reason) {
    super(HttpStatus.INTERNAL_SERVER_ERROR, reason);
  }

}
