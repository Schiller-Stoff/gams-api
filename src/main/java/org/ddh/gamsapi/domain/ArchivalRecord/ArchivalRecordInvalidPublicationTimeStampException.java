package org.ddh.gamsapi.domain.ArchivalRecord;

import org.springframework.http.HttpStatus;

/**
 * Indicates error states where the publication timestamp of an archival record was wrong.
 */
public class ArchivalRecordInvalidPublicationTimeStampException extends ArchivalRecordException {
  public ArchivalRecordInvalidPublicationTimeStampException(String reason) {
    super(HttpStatus.BAD_REQUEST, reason);
  }

  public ArchivalRecordInvalidPublicationTimeStampException(String reason, Throwable cause) {
    super(HttpStatus.BAD_REQUEST, reason, cause);
  }
}
