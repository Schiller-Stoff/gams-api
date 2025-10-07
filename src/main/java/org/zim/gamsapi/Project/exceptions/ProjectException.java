package org.zim.gamsapi.Project.exceptions;

import org.springframework.http.HttpStatusCode;
import org.springframework.web.server.ResponseStatusException;

/**
 * Exceptions related to GAMS-projects
 */
public class ProjectException extends ResponseStatusException {
  public ProjectException(HttpStatusCode status, String reason) {
    super(status, reason);
  }
}
