package org.zim.gamsapi.infrastructure.System.security.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Authorization denied because a user is assigned to a project BUT missing required role rights.
 */
public class UserAssignedToProjectButMissingEditorRoleException extends SecurityException{
  public UserAssignedToProjectButMissingEditorRoleException(String reason) {
    super(HttpStatus.FORBIDDEN, reason);
  }
}
