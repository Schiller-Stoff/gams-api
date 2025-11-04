package org.ddh.gamsapi.domain.Project.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Represents state where a project already exists.
 */
public class ProjectAlreadyExistsException extends  ProjectException {

  public ProjectAlreadyExistsException(String reason) {
    super(HttpStatus.CONFLICT, reason);
  }

}
