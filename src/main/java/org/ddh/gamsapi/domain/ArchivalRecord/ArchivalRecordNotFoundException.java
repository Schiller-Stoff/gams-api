package org.ddh.gamsapi.domain.ArchivalRecord;

import org.ddh.gamsapi.infrastructure.System.exceptions.GamsApiException;
import org.springframework.http.HttpStatus;

/**
 * Represents error states where the archival record was not found.
 */
public class ArchivalRecordNotFoundException extends GamsApiException {

  public ArchivalRecordNotFoundException(String reason) {
    super(HttpStatus.NOT_FOUND, reason);
  }

  public ArchivalRecordNotFoundException(String reason, Throwable cause) {
    super(HttpStatus.NOT_FOUND, reason, cause);
  }

}
