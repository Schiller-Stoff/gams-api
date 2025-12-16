package org.ddh.gamsapi.infrastructure.User.exceptions;

import org.springframework.http.HttpStatus;

public class UserNotFoundException extends UserException {
  public UserNotFoundException(String reason) {
    super(HttpStatus.NOT_FOUND, reason);
  }

  public UserNotFoundException(String reason, Throwable cause) {
    super(HttpStatus.NOT_FOUND, reason, cause);
  }

}
