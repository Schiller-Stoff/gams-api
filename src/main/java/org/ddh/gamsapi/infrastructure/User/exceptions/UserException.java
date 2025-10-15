package org.ddh.gamsapi.infrastructure.User.exceptions;

import org.springframework.http.HttpStatusCode;
import org.springframework.web.server.ResponseStatusException;

public class UserException extends ResponseStatusException {
  public UserException(HttpStatusCode status, String reason) {
    super(status, reason);
  }

}
