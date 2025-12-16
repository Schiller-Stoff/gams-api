package org.ddh.gamsapi.domain.Project.exceptions;

import org.ddh.gamsapi.infrastructure.System.exceptions.GamsApiException;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.server.ResponseStatusException;

/**
 * Exceptions related to GAMS-projects
 */
public class ProjectException extends GamsApiException {
  public ProjectException(HttpStatusCode status, String reason) {
    super(status, reason);
  }

  public ProjectException(HttpStatusCode status, String reason, Throwable cause) {
    super(status, reason, cause);
  }
}
