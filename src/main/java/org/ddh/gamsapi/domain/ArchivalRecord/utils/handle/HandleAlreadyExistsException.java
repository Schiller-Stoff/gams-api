package org.ddh.gamsapi.domain.ArchivalRecord.utils.handle;

import org.ddh.gamsapi.domain.ArchivalRecord.utils.exceptions.ArchivalRecordException;
import org.springframework.http.HttpStatus;

public class HandleAlreadyExistsException extends ArchivalRecordException {
  public HandleAlreadyExistsException(String reason) {
    super(HttpStatus.INTERNAL_SERVER_ERROR, reason);
  }
}
