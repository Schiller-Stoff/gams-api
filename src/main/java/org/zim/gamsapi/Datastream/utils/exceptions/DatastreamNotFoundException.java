package org.zim.gamsapi.Datastream.utils.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Represents error state where the datastream was not found.
 */
public class DatastreamNotFoundException  extends DatastreamException {
  public DatastreamNotFoundException(String reason) {
    super(HttpStatus.NOT_FOUND, reason);
  }
}
