package org.zim.gamsapi.SubInfoPack.exceptions;

import org.springframework.http.HttpStatusCode;
import org.springframework.web.server.ResponseStatusException;

/**
 * Error states concerning SubmissionInformationPackages
 */
public class SubInfoPackException extends ResponseStatusException {
  public SubInfoPackException(HttpStatusCode status, String reason) {
    super(status, reason);
  }
}
