package org.ddh.gamsapi.application.Ingest.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Represents error states related to processes done for Submission Information Packages,
 * like unzipping the zipped data or parsing the contained metadata file.
 */
public class IngestProcessingException extends IngestException {

  public IngestProcessingException(String reason) {
    super(HttpStatus.INTERNAL_SERVER_ERROR, reason);
  }

  public IngestProcessingException(String reason, Throwable cause) {
    super(HttpStatus.INTERNAL_SERVER_ERROR, reason, cause);
  }

}
