package org.zim.gamsapi.application.Ingest.exceptions;

import org.springframework.http.HttpStatus;

public class ExportProcessingException extends ExportException {
  public ExportProcessingException(String reason) {
    super(HttpStatus.INTERNAL_SERVER_ERROR, reason);
  }
}
