package org.ddh.gamsapi.application.Ingest.exceptions;

import org.ddh.gamsapi.infrastructure.System.exceptions.GamsApiException;
import org.springframework.http.HttpStatusCode;

/**
 * Represents error states that occur during export operations within the application.
 * This exception is used to signal issues such as failures in exporting data,
 * invalid export parameters, or other export-related errors.
 */
public class ExportException extends GamsApiException {
  public ExportException(HttpStatusCode status, String reason) {
    super(status, reason);
  }

  public ExportException(HttpStatusCode status, String reason, Throwable cause) {
    super(status, reason, cause);
  }
}
