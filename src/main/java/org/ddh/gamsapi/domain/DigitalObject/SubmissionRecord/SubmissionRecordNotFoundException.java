package org.ddh.gamsapi.domain.DigitalObject.SubmissionRecord;

import org.ddh.gamsapi.domain.DigitalObject.utils.exceptions.DigitalObjectException;
import org.springframework.http.HttpStatus;

/**
 * Represents states where a submission records does not exist
 */
public class SubmissionRecordNotFoundException extends DigitalObjectException {
  public SubmissionRecordNotFoundException(String message) {
    super(HttpStatus.NOT_FOUND, message);
  }

  public SubmissionRecordNotFoundException(String message, Throwable cause) {
    super(HttpStatus.NOT_FOUND,message, cause);
  }

}
