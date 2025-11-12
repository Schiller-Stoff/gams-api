package org.ddh.gamsapi.application.Integration.PlexusSearch.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a query violates security or complexity rules.
 */
public class PlexusSearchForbiddenQueryException extends PlexusSearchException {
  public PlexusSearchForbiddenQueryException(String message) {
    super(HttpStatus.FORBIDDEN, message);
  }
}
