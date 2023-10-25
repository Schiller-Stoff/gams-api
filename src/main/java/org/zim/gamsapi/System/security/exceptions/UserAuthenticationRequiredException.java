package org.zim.gamsapi.System.security.exceptions;

import org.springframework.http.HttpStatus;

/**
 * If user authentication failed.
 */
public class UserAuthenticationRequiredException extends SecurityException {

  public UserAuthenticationRequiredException(String reason) {
    super(HttpStatus.UNAUTHORIZED, reason);
  }

}
