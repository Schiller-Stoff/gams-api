package org.ddh.gamsapi.application.Ingest.exceptions;

import org.ddh.gamsapi.infrastructure.System.exceptions.GamsApiException;
import org.springframework.http.HttpStatusCode;

/**
 * Error states concerning SubmissionInformationPackages
 */
public class IngestException extends GamsApiException {
  public IngestException(HttpStatusCode status, String reason) {
    super(status, reason);
  }

  public IngestException(HttpStatusCode status, String reason, Throwable cause) {
    super(status, reason, cause);
  }
}
