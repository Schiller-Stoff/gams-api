package org.ddh.gamsapi.domain.ArchivalRecord.utils.exceptions;

import org.ddh.gamsapi.domain.ArchivalRecord.ArchivalRecordException;
import org.springframework.http.HttpStatus;

/**
 * Indicates error states where more than one active archival records exist!
 */
public class ArchivalRecordInconsistentActiveRecordsException extends ArchivalRecordException {
  public ArchivalRecordInconsistentActiveRecordsException(String reason) {
    super(HttpStatus.INTERNAL_SERVER_ERROR, reason);
  }
}
