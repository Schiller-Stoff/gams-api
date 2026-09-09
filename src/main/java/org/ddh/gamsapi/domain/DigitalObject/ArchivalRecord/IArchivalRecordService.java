package org.ddh.gamsapi.domain.DigitalObject.ArchivalRecord;

import java.util.List;

/**
 * Service interface for managing ArchivalRecord entities.
 */
public interface IArchivalRecordService {

  /**
   * Find ArchivalRecord by DigitalObject ID.
   * @param digitalObjectId the ID of the associated DigitalObject
   * @return the ArchivalRecord
   */
  List<ArchivalRecordCompactView> findForObject(String digitalObjectId);


  /**
   * Find ArchivalRecord by digital object id and archiving status.
   * @param digitalObjectId id of the associated object
   * @param archivingStatus records to be found by status
   * @return list of found archival records
   */
  List<ArchivalRecordCompactView> findForObjectByArchivingStatus(String digitalObjectId, ArchivingStatus archivingStatus);

  /**
   * Saves given archival record bia generating a id + checking uniqueness of the pid + assigning archival status reserved.
   * @param objectId object's archival record that should be drafted.
   * @param archivalRecordReserveDto the ArchivalRecord to save
   * @return the saved ArchivalRecord
   */
  ArchivalRecord reserve(String objectId, ArchivalRecordReserveDto archivalRecordReserveDto);

  /**
   * Deletes an archival record by it's id.
   * @param archivalRecordId id of the archival record.
   */
  void deleteById(Long archivalRecordId);

  /**
   * Updates an existing archival record
   * @param objectId object's archival record that should be drafted.
   * @param archivalRecordDraftDto contains data about archival record to be updated.
   */
  void draftArchivalRecord(String objectId, ArchivalRecordDraftDto archivalRecordDraftDto);

  /**
   * Publishes an existing, active archival record
   * @param objectId object's archival record that should be published.
   * @param archivalRecordPublishDto contains data necessary for publishing process.
   */
  void publishArchivalRecord(String objectId, ArchivalRecordPublishDto archivalRecordPublishDto);

}
