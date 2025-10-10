package org.zim.gamsapi.Datastream.utils.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Represents error state where a datastream's file cannot be written to a file (on the filesystem).
 */
public class DatastreamCannotWriteFileException extends DatastreamException {

  public DatastreamCannotWriteFileException(String reason) {
    super(HttpStatus.INTERNAL_SERVER_ERROR, reason);
  }

}
