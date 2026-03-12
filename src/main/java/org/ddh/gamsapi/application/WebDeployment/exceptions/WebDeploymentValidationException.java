package org.ddh.gamsapi.application.WebDeployment.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Thrown when the uploaded zip fails validation
 * (empty, too large, contains invalid entries).
 */
public class WebDeploymentValidationException extends WebDeploymentException {

  public WebDeploymentValidationException(String reason) {
    super(HttpStatus.BAD_REQUEST, reason);
  }
}