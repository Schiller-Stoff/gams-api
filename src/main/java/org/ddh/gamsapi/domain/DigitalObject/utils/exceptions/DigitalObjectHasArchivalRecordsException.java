package org.ddh.gamsapi.domain.DigitalObject.utils.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Thrown when attempting to delete a digital object that still has archival records.
 * Archival records document provenance in the archive and must not be silently
 * discarded as a side effect of deleting the digital object.
 */
public class DigitalObjectHasArchivalRecordsException extends DigitalObjectException {

  public DigitalObjectHasArchivalRecordsException(String reason) {
    super(HttpStatus.CONFLICT, reason);
  }

  public DigitalObjectHasArchivalRecordsException(String reason, Throwable cause) {
    super(HttpStatus.CONFLICT, reason, cause);
  }
}