package org.ddh.gamsapi.domain.Datastream.utils.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

/**
 * Thrown when attempting to create a datastream with a dsid
 * that already exists on the digital object.
 */
public class DatastreamAlreadyExistsException extends DatastreamException {

  public DatastreamAlreadyExistsException(String reason) {
    super(HttpStatus.CONFLICT, reason);
  }

  public DatastreamAlreadyExistsException(String reason, Throwable cause) {
    super(HttpStatus.CONFLICT, reason, cause);
  }
}