package org.ddh.gamsapi.application.WebDeployment.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Thrown when filesystem operations fail during web deployment.
 */
public class WebDeploymentStorageException extends WebDeploymentException {

  public WebDeploymentStorageException(String reason) {
    super(HttpStatus.INTERNAL_SERVER_ERROR, reason);
  }

  public WebDeploymentStorageException(String reason, Throwable cause) {
    super(HttpStatus.INTERNAL_SERVER_ERROR, reason, cause);
  }
}