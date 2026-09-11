package org.ddh.gamsapi.domain.ArchivalRecord;

import org.ddh.gamsapi.infrastructure.System.exceptions.GamsApiException;
import org.springframework.http.HttpStatus;

/**
 * Indicates that the pid (handle / doi) is invalid
 */
public class ArchivalRecordInvalidPidException extends GamsApiException {
  public ArchivalRecordInvalidPidException(String reason) {
    super(HttpStatus.BAD_REQUEST, reason);
  }
}
