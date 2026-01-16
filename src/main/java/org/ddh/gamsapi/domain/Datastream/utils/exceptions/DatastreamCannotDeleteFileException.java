package org.ddh.gamsapi.domain.Datastream.utils.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.transaction.TransactionSystemException;

/**
 * Represents error state where a datastream's file cannot be deleted from the filesystem.
 */
// TODO extending TransactionSystemException looks bad here!
public class DatastreamCannotDeleteFileException extends TransactionSystemException {

  public DatastreamCannotDeleteFileException(String reason) {
    super(reason);
  }

  public DatastreamCannotDeleteFileException(String reason, Throwable cause) {
    super(reason, cause);
  }

}
