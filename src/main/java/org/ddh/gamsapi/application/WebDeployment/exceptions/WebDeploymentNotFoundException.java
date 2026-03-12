package org.ddh.gamsapi.application.WebDeployment.exceptions;

import org.springframework.http.HttpStatus;

public class WebDeploymentNotFoundException extends WebDeploymentException {

  public WebDeploymentNotFoundException(String reason) {
    super(HttpStatus.NOT_FOUND, reason);
  }
}