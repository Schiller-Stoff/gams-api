package org.ddh.gamsapi.application.Ingest.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Represents error states where an object is in an unexpected state during an export operation.
 * e.g. object contains invalid data, is missing required fields, or is otherwise not in a state that allows
 * the export to proceed.
 */
public class ExportUnexpectedObjectStateException extends ExportException {
  public ExportUnexpectedObjectStateException(String reason) {
    super(HttpStatus.INTERNAL_SERVER_ERROR, reason);
  }

  public ExportUnexpectedObjectStateException(String reason, Throwable cause) {
    super(HttpStatus.INTERNAL_SERVER_ERROR, reason, cause);
  }
}
