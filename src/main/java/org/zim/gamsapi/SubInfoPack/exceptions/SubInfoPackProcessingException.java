package org.zim.gamsapi.SubInfoPack.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.server.ResponseStatusException;

/**
 * Represents error states related to processes done for Submission Information Packages,
 * like unzipping the zipped data or parsing the contained metadata file.
 */
public class SubInfoPackProcessingException extends ResponseStatusException {

  public SubInfoPackProcessingException(String reason) {
    super(HttpStatus.INTERNAL_SERVER_ERROR, reason);
  }
}
