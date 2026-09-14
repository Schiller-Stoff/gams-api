package org.ddh.gamsapi.domain.ArchivalRecord;

import org.springframework.http.HttpStatus;

/**
 * Indicates that the pid (handle / doi) is invalid
 */
public class ArchivalRecordInvalidPidException extends ArchivalRecordException {
  public ArchivalRecordInvalidPidException(String reason) {
    super(HttpStatus.BAD_REQUEST, reason);
  }
}
