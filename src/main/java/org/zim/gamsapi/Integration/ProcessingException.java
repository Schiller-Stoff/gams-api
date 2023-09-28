package org.zim.gamsapi.Integration;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Represents error states related to data processing, e.g. if a file could not be parsed.
 */
public class ProcessingException extends ResponseStatusException {

  public ProcessingException(String reason) {
    super(HttpStatus.INTERNAL_SERVER_ERROR, reason);
  }

}
