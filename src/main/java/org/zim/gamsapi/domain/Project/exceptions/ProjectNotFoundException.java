package org.zim.gamsapi.domain.Project.exceptions;

import org.springframework.http.HttpStatus;

/**
 * GAMS project not found - error 404.
 */
public class ProjectNotFoundException extends ProjectException {

  public ProjectNotFoundException(String reason) {
    super(HttpStatus.NOT_FOUND, reason);
  }

}
