package org.ddh.gamsapi.application.Ingest.exceptions;

import org.springframework.http.HttpStatus;

public class ExportProcessingException extends ExportException {
  public ExportProcessingException(String reason) {
    super(HttpStatus.INTERNAL_SERVER_ERROR, reason);
  }

  public ExportProcessingException(String reason, Throwable cause) {
    super(HttpStatus.INTERNAL_SERVER_ERROR, reason, cause);
  }
}
