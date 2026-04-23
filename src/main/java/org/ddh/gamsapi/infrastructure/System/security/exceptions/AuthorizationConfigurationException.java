package org.ddh.gamsapi.infrastructure.System.security.exceptions;

import org.ddh.gamsapi.infrastructure.System.exceptions.GamsApiException;
import org.springframework.http.HttpStatus;

/**
 * Authorization configuration exception.
 * This exception is thrown when the authorization configuration is not correct.
 */
public class AuthorizationConfigurationException extends GamsApiException {
  public AuthorizationConfigurationException( String reason) {
    super(HttpStatus.INTERNAL_SERVER_ERROR, reason);
  }

  public AuthorizationConfigurationException( String reason, Throwable cause) {
    super(HttpStatus.INTERNAL_SERVER_ERROR, reason, cause);
  }
}
