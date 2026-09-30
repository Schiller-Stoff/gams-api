package org.ddh.gamsapi.domain.ArchivalRecord.utils.handle;

import org.springframework.http.HttpStatus;

/**
 * Represents error states
 */
public class HandleServerNotReachableException extends HandleServerException {
  public HandleServerNotReachableException(String message) {
    super(HttpStatus.INTERNAL_SERVER_ERROR, message);
  }

  public HandleServerNotReachableException(String reason, Throwable cause){
    super(HttpStatus.INTERNAL_SERVER_ERROR, reason, cause);
  }
}
