package org.ddh.gamsapi.domain.ArchivalRecord.utils.handle;

import org.springframework.http.HttpStatus;

public class HandleNotRegisteredException extends HandleServerException {
  public HandleNotRegisteredException(String message) {
    super(HttpStatus.INTERNAL_SERVER_ERROR, message);
  }
}
