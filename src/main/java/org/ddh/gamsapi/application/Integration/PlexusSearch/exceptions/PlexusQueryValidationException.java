package org.ddh.gamsapi.application.Integration.PlexusSearch.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a Plexus query violates security or complexity rules.
 */
public class PlexusQueryValidationException extends PlexusSearchException {
  public PlexusQueryValidationException(String reason) {
    super(HttpStatus.BAD_REQUEST, reason);
  }

  public PlexusQueryValidationException(String reason, Throwable cause) {
    super(HttpStatus.BAD_REQUEST, reason, cause);
  }
}
