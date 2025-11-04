package org.ddh.gamsapi.application.Integration.Common.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Represents error states related to integration, e.g. if a request to an external service fails or etc.
 */
public class IntegrationServiceException extends IntegrationException {

  public IntegrationServiceException(String reason) {
    super(HttpStatus.INTERNAL_SERVER_ERROR, reason);
  }

}
