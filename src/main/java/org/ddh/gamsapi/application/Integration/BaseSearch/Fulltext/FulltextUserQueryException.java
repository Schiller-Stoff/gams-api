package org.ddh.gamsapi.application.Integration.BaseSearch.Fulltext;

import org.ddh.gamsapi.application.Integration.Common.exceptions.IntegrationUserQueryException;

/**
 * Exception for invalid user queries in fulltext search.
 */
public class FulltextUserQueryException extends IntegrationUserQueryException {
  public FulltextUserQueryException(String reason) {
    super(reason);
  }
}
