package org.zim.gamsapi.infrastructure.User.exceptions;

import org.springframework.http.HttpStatus;

public class UserNotFoundException extends UserException {
  public UserNotFoundException(String reason) {
    super(HttpStatus.NOT_FOUND, reason);
  }

}
