package org.zim.gamsapi.infrastructure.System.security.exceptions;

import org.springframework.http.HttpStatus;

/**
 * General exception for when a user is not authorized to access a specific endpoint
 */
public class UserNotAuthorizedException extends SecurityException {

  public UserNotAuthorizedException(String reason) {
    super(HttpStatus.FORBIDDEN, reason);
  }
}
