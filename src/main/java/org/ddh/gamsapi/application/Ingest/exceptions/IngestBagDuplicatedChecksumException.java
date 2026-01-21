package org.ddh.gamsapi.application.Ingest.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Represents error states where the manifest checksum files in the bag contain duplicate checksums.
 */
public class IngestBagDuplicatedChecksumException extends IngestException {

  public IngestBagDuplicatedChecksumException(String reason) {
    super(HttpStatus.BAD_REQUEST, reason);
  }

  public  IngestBagDuplicatedChecksumException(String reason, Throwable cause) {
    super(HttpStatus.BAD_REQUEST, reason, cause);
  }
}
