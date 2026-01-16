package org.ddh.gamsapi.domain.Project.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Exception representing: Requesting a project with given invalid date format.
 */
public class ProjectInvalidDateFormatException extends ProjectException {
  public ProjectInvalidDateFormatException(String message) {
    super(HttpStatus.BAD_REQUEST, message);
  }

  public ProjectInvalidDateFormatException(String message, Throwable cause) {
    super(HttpStatus.BAD_REQUEST, message, cause);
  }
}
