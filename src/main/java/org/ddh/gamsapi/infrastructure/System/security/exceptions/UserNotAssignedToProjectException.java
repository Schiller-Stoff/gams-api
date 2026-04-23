package org.ddh.gamsapi.infrastructure.System.security.exceptions;

import org.springframework.security.access.AccessDeniedException;

/**
 * Authorization denied because a user is not assigned to a project.
 */
public class UserNotAssignedToProjectException extends AccessDeniedException {

  public UserNotAssignedToProjectException(String msg) {
    super(msg);
  }

  public UserNotAssignedToProjectException(String msg, Throwable cause) {
    super(msg, cause);
  }
}
