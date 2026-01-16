package org.ddh.gamsapi.domain.Project.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Represents state where a project already exists.
 */
public class ProjectAlreadyExistsException extends  ProjectException {

  public ProjectAlreadyExistsException(String reason) {
    super(HttpStatus.CONFLICT, reason);
  }

  public ProjectAlreadyExistsException(String reason, Throwable cause) {
    super(HttpStatus.CONFLICT, reason, cause);
  }

}
