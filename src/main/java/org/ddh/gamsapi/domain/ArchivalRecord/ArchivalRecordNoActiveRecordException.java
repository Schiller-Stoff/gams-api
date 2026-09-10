package org.ddh.gamsapi.domain.ArchivalRecord;

import org.ddh.gamsapi.domain.DigitalObject.utils.exceptions.DigitalObjectException;
import org.springframework.http.HttpStatus;

/**
 * Represents error states where an active archival record is required - but none was found.
 */
public class ArchivalRecordNoActiveRecordException extends DigitalObjectException {

  public ArchivalRecordNoActiveRecordException(String reason) {
    super(HttpStatus.BAD_REQUEST, reason);
  }

  public ArchivalRecordNoActiveRecordException(String reason, Throwable cause) {
    super(HttpStatus.BAD_REQUEST, reason, cause);
  }

}
