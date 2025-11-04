package org.ddh.gamsapi.domain.Datastream.utils.exceptions;

import org.springframework.http.HttpStatusCode;
import org.springframework.web.server.ResponseStatusException;

/**
 * General runtime exception concerning datastreams.
 */
public class DatastreamException extends ResponseStatusException {
  public DatastreamException(HttpStatusCode status, String reason) {
    super(status, reason);
  }
}
