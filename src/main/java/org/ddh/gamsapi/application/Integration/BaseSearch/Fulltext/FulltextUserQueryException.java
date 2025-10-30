package org.ddh.gamsapi.application.Integration.BaseSearch.Fulltext;

import org.ddh.gamsapi.application.Integration.Common.exceptions.IntegrationException;
import org.springframework.http.HttpStatus;

/**
 * Exception for invalid user queries in fulltext search.
 */
public class FulltextUserQueryException extends IntegrationException {
  public FulltextUserQueryException(String reason) {
    super(HttpStatus.BAD_REQUEST, reason);
  }
}
