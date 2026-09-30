package org.ddh.gamsapi.domain.ArchivalRecord.utils.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Represents error states where an archival record was not found.
 */
public class ArchivalRecordAlreadyExistsException extends ArchivalRecordException {
  public ArchivalRecordAlreadyExistsException(String reason) {
    super(HttpStatus.BAD_REQUEST, reason);
  }
}
