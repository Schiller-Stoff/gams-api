package org.ddh.gamsapi.infrastructure.System.security.exceptions;

import org.springframework.security.access.AccessDeniedException;

/**
 * General exception for when a user is not authorized to access a specific endpoint
 */
public class UserNotAuthorizedException extends AccessDeniedException {

  public UserNotAuthorizedException(String msg) {
    super(msg);
  }

  public UserNotAuthorizedException(String msg, Throwable cause) {
    super(msg, cause);
  }
}
