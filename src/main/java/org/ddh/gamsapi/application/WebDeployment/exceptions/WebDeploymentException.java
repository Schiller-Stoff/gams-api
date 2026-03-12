package org.ddh.gamsapi.application.WebDeployment.exceptions;

import org.ddh.gamsapi.infrastructure.System.exceptions.GamsApiException;
import org.springframework.http.HttpStatusCode;

/**
 * Base exception for web deployment operations.
 */
public class WebDeploymentException extends GamsApiException {

  public WebDeploymentException(HttpStatusCode status, String reason) {
    super(status, reason);
  }

  public WebDeploymentException(HttpStatusCode status, String reason, Throwable cause) {
    super(status, reason, cause);
  }
}