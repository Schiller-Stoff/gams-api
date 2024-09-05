package org.zim.gamsapi.Datastream.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Represents error state where a datastream's file cannot be deleted from the filesystem.
 */
public class DatastreamCannotDeleteFileException extends DatastreamException {

  public DatastreamCannotDeleteFileException(String reason) {
    super(HttpStatus.INTERNAL_SERVER_ERROR, reason);
  }

}
