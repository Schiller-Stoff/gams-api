package org.ddh.gamsapi.domain.ArchivalRecord;

import org.ddh.gamsapi.domain.DigitalObject.utils.exceptions.DigitalObjectException;
import org.springframework.http.HttpStatus;

/**
 * Indicates an error state where an archival record of blocking type already exists - so that no further archival record may be created.
 */
public class ArchivalRecordAlreadyActiveException extends DigitalObjectException {
  public ArchivalRecordAlreadyActiveException(String reason) {
    super(HttpStatus.CONFLICT, reason);
  }
  public ArchivalRecordAlreadyActiveException(String reason, Throwable cause) {
    super(HttpStatus.CONFLICT, reason, cause);
  }
}
