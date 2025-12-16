package org.ddh.gamsapi.application.Integration.Common.exceptions;

import org.ddh.gamsapi.infrastructure.System.exceptions.GamsApiException;
import org.springframework.http.HttpStatusCode;

/**
 * Represents error states related to integration, e.g. if the data processing fails in the api or if a request to an external service fails or etc.
 * TODO let ProcessingException extend this class
 * TODO rename ProcessingException to something more fitting - e.g. IntegrationDataProcessingException
 */
public class IntegrationException extends GamsApiException {

  public IntegrationException(HttpStatusCode status, String reason) {
    super(status, reason);
  }

  public IntegrationException(HttpStatusCode status, String reason, Throwable cause) {
    super(status, reason, cause);
  }

}
