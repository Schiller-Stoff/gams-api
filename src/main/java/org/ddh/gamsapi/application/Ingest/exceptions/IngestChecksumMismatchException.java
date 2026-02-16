package org.ddh.gamsapi.application.Ingest.exceptions;

import org.springframework.http.HttpStatus;

public class IngestChecksumMismatchException extends IngestException {

  public IngestChecksumMismatchException(String reason) {
    super(HttpStatus.UNPROCESSABLE_CONTENT, reason);
  }

  public IngestChecksumMismatchException(String reason, Throwable cause) {
    super(HttpStatus.UNPROCESSABLE_CONTENT, reason, cause);
  }

}
