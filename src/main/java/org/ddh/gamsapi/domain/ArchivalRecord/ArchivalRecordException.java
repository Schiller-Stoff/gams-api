package org.ddh.gamsapi.domain.ArchivalRecord;

import org.ddh.gamsapi.infrastructure.System.exceptions.GamsApiException;
import org.springframework.http.HttpStatusCode;

public class ArchivalRecordException extends GamsApiException {
  public ArchivalRecordException(HttpStatusCode status, String reason) {
    super(status, reason);
  }

  public ArchivalRecordException(HttpStatusCode status, String reason, Throwable cause) {
    super(status, reason, cause);
  }
}
