package org.ddh.gamsapi.domain.Project.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Thrown when attempting to delete a project that still contains digital objects.
 */
public class ProjectNotEmptyException extends ProjectException {

  public ProjectNotEmptyException(String reason) {
    super(HttpStatus.CONFLICT, reason);
  }

  public ProjectNotEmptyException(String reason, Throwable cause) {
    super(HttpStatus.CONFLICT, reason, cause);
  }
}