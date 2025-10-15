package org.zim.gamsapi.domain.Project.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Represents an error state where a client requests a digital object that does not match the expected project.
 * E.g. specifying the object hsa.1 from project demo
 */
public class ProjectObjectMismatchException extends ProjectException {
  public  ProjectObjectMismatchException(String reason) {
    super(HttpStatus.CONFLICT, reason);
  }
}
