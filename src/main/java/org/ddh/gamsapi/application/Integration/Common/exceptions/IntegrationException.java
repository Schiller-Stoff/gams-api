package org.ddh.gamsapi.application.Integration.Common.exceptions;

import org.springframework.http.HttpStatusCode;
import org.springframework.web.server.ResponseStatusException;

/**
 * Represents error states related to integration, e.g. if the data processing fails in the api or if a request to an external service fails or etc.
 * TODO let ProcessingException extend this class
 * TODO rename ProcessingException to something more fitting - e.g. IntegrationDataProcessingException
 */
public class IntegrationException extends ResponseStatusException {

  public IntegrationException(HttpStatusCode status, String reason) {
    super(status, reason);
  }

}
