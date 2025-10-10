package org.zim.gamsapi.Ingest.exceptions;

import org.springframework.http.HttpStatusCode;
import org.springframework.web.server.ResponseStatusException;

/**
 * Error states concerning SubmissionInformationPackages
 */
public class IngestException extends ResponseStatusException {
  public IngestException(HttpStatusCode status, String reason) {
    super(status, reason);
  }
}
