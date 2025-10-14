package org.zim.gamsapi.domain.DigitalObject.SubmissionRecord;

/**
 * Service interface for managing SubmissionRecord entities.
 */
public interface ISubmissionRecordService {

  /**
   * Find SubmissionRecord by DigitalObject ID.
   * @param digitalObjectId the ID of the associated DigitalObject
   * @return the SubmissionRecord
   */
  SubmissionRecord find(String digitalObjectId);

}
