package org.ddh.gamsapi.application.Integration.PlexusSearch.exceptions;

import org.ddh.gamsapi.application.Integration.Common.exceptions.IntegrationException;
import org.springframework.http.HttpStatus;

/**
 * General exception for Plexus Search integration errors.
 */
public class PlexusSearchException extends IntegrationException {
  public PlexusSearchException(HttpStatus status, String reason) {
    super(status, reason);
  }
}
