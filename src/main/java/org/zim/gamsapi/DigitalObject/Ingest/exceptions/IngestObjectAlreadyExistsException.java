package org.zim.gamsapi.DigitalObject.Ingest.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Represents ingest failures when a digital object already exists.
 */
public class IngestObjectAlreadyExistsException extends IngestException {
  public IngestObjectAlreadyExistsException(String reason) {
    super(HttpStatus.CONFLICT, reason);
  }
}
