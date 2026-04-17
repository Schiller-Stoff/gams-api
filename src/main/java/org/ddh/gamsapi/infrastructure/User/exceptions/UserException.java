package org.ddh.gamsapi.infrastructure.User.exceptions;

import org.ddh.gamsapi.infrastructure.System.exceptions.GamsApiException;
import org.springframework.http.HttpStatusCode;

public class UserException extends GamsApiException {
  public UserException(HttpStatusCode status, String reason) {
    super(status, reason);
  }

  public UserException(HttpStatusCode status, String reason, Throwable cause) {
    super(status, reason, cause);
  }

}
