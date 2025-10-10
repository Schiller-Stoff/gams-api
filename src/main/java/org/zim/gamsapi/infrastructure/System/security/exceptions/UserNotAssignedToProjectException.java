package org.zim.gamsapi.infrastructure.System.security.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Authorization denied because a user is not assigned to a project.
 */
public class UserNotAssignedToProjectException extends SecurityException {

  public UserNotAssignedToProjectException(String reason) {
    super(HttpStatus.FORBIDDEN, reason);
  }

}
