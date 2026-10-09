package org.ddh.gamsapi.domain.ArchivalRecord.utils.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Represents error states where the reference between digital object and an archival record
 * is inconsistent. (e.g. the digital object did change unexpectedly)
 */
public class ArchivalRecordDigitalObjectInconsistencyException extends ArchivalRecordException {
  public ArchivalRecordDigitalObjectInconsistencyException(String reason) {
    super(HttpStatus.UNPROCESSABLE_CONTENT, reason);
  }


}
