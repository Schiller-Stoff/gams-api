package org.zim.gamsapi.Datastream.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.transaction.TransactionSystemException;

/**
 * Represents error state where a datastream's file cannot be deleted from the filesystem.
 */
public class DatastreamCannotDeleteFileException extends TransactionSystemException {

  public DatastreamCannotDeleteFileException(String reason) {
    super(reason);
  }

}
