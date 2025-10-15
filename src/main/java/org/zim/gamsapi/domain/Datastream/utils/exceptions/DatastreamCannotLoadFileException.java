package org.zim.gamsapi.domain.Datastream.utils.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Represents error states where a datastream's file cannot be loaded from the filesystem.
 */
public class DatastreamCannotLoadFileException extends DatastreamException {
  public DatastreamCannotLoadFileException(String reason) {
    super(HttpStatus.INTERNAL_SERVER_ERROR, reason);
  }

}
