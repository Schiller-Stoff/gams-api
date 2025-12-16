package org.ddh.gamsapi.domain.Datastream.utils.exceptions;

import org.ddh.gamsapi.infrastructure.System.exceptions.GamsApiException;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.server.ResponseStatusException;

/**
 * General runtime exception concerning datastreams.
 */
public class DatastreamException extends GamsApiException {
  public DatastreamException(HttpStatusCode status, String reason) {
    super(status, reason);
  }

  public DatastreamException(HttpStatusCode status, String reason, Throwable cause) {
    super(status, reason, cause);
  }

}
