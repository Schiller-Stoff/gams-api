package org.ddh.gamsapi.application.Ingest.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when an ingest operation is attempted against a different project defined in the sip.json
 */
public class IngestAgainstDifferentProjectException extends IngestException {

  public IngestAgainstDifferentProjectException(String reason) {
    super(HttpStatus.BAD_REQUEST, reason);
  }

  public IngestAgainstDifferentProjectException(String reason, Throwable cause) {
    super(HttpStatus.BAD_REQUEST, reason, cause);
  }



}
