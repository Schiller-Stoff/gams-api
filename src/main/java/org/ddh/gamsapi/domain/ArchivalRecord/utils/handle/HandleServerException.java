package org.ddh.gamsapi.domain.ArchivalRecord.utils.handle;

import org.ddh.gamsapi.domain.ArchivalRecord.utils.exceptions.ArchivalRecordException;
import org.springframework.http.HttpStatusCode;

public class HandleServerException extends ArchivalRecordException {
  public HandleServerException(HttpStatusCode status, String reason) {
    super(status,reason);
  }
  public HandleServerException(HttpStatusCode status, String reason, Throwable cause) {
    super(status, reason, cause);
  }
}
