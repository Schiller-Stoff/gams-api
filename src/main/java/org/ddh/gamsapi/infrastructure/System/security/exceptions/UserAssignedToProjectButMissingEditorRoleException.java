package org.ddh.gamsapi.infrastructure.System.security.exceptions;

import org.springframework.security.access.AccessDeniedException;

/**
 * Authorization denied because a user is assigned to a project BUT missing required role rights.
 */
public class UserAssignedToProjectButMissingEditorRoleException extends AccessDeniedException {
  public UserAssignedToProjectButMissingEditorRoleException(String msg) {
    super(msg);
  }

  public UserAssignedToProjectButMissingEditorRoleException(String msg, Throwable cause) {
    super(msg, cause);
  }
}
