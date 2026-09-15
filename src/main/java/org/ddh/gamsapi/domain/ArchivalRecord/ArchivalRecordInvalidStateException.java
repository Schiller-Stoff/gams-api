package org.ddh.gamsapi.domain.ArchivalRecord;

import org.springframework.http.HttpStatus;

/**
 *  Indicates an error where an archival record is in an invalid state.
 *  E.g. there cannot be a publication state without record id assigned.
 */
public class ArchivalRecordInvalidStateException extends ArchivalRecordException {
  public ArchivalRecordInvalidStateException(String reason) {
    super(HttpStatus.BAD_REQUEST, reason);
  }
}
