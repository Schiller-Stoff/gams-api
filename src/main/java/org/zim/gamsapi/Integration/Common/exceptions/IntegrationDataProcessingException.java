package org.zim.gamsapi.Integration.Common.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Represents error states related to data processing, e.g. if a file could not be parsed.
 */
public class IntegrationDataProcessingException extends IntegrationException {

  public IntegrationDataProcessingException(String reason) {
    super(HttpStatus.INTERNAL_SERVER_ERROR, reason);
  }

}
