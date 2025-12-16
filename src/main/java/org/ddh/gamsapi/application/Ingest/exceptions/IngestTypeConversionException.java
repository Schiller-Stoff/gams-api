package org.ddh.gamsapi.application.Ingest.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Represents all exceptions that occur during type conversion in the ingest process.
 * E.g. when mapping a bagit sip.json to a digital object.

 */
public class IngestTypeConversionException extends IngestProcessingException {
  public IngestTypeConversionException(String reason) {
    super(reason);
  }

  public IngestTypeConversionException(String reason, Throwable cause) {
    super(reason, cause);
  }

}
