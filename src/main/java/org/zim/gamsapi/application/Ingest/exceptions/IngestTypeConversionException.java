package org.zim.gamsapi.application.Ingest.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Represents all exceptions that occur during type conversion in the ingest process.
 * E.g. when mapping a bagit sip.json to a digital object.

 */
public class IngestTypeConversionException extends IngestException {
  public IngestTypeConversionException(String reason) {
    super(HttpStatus.INTERNAL_SERVER_ERROR, reason);
  }
}
