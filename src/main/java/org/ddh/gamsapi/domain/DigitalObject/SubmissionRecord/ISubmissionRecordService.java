package org.ddh.gamsapi.domain.DigitalObject.SubmissionRecord;

import java.util.Optional;

/**
 * Service interface for managing SubmissionRecord entities.
 */
public interface ISubmissionRecordService {

  /**
   * Find SubmissionRecord by DigitalObject ID.
   * @param digitalObjectId the ID of the associated DigitalObject
   * @return the SubmissionRecord
   */
  Optional<SubmissionRecord> find(String digitalObjectId);

}
