package org.ddh.gamsapi.domain.ArchivalRecord.utils.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Represents error states where the archival record was not found.
 */
public class ArchivalRecordNotFoundException extends ArchivalRecordException {

  public ArchivalRecordNotFoundException(String reason) {
    super(HttpStatus.NOT_FOUND, reason);
  }

  public ArchivalRecordNotFoundException(String reason, Throwable cause) {
    super(HttpStatus.NOT_FOUND, reason, cause);
  }

}
