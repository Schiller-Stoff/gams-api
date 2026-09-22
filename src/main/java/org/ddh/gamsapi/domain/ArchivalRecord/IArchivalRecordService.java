package org.ddh.gamsapi.domain.ArchivalRecord;

import org.ddh.gamsapi.domain.ArchivalRecord.utils.dto.ArchivalRecordDraftDto;
import org.ddh.gamsapi.domain.ArchivalRecord.utils.dto.ArchivalRecordPublishDto;
import org.ddh.gamsapi.infrastructure.System.dto.PagedResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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
   * Deletes an archival record by it's id.
   * @param archivalRecordPid pid of the archival record.
   */
  void deleteById(String archivalRecordPid);

  /**
   * Creates an archival record by also specifying a linked digital object.
   * @param objectId id of linked object
   * @return archival record
   */
  ArchivalRecord reserveArchivalRecordByObjectId(String objectId);

  /**
   * Creates an archival record with given pid linked to specified digital object (in reserved state)
   * @param objectId id of the linked object
   * @param pid pid of the archival record
   * @return archival record entity
   */
  ArchivalRecord reserveArchivalRecordByObjectIdAndPid(String objectId, String pid);

  /**
   * Create an archival record in reserve state
   * @return archival record entity
   */
  ArchivalRecord reserveArchivalRecord();

  /**
   * Create an archival record via specifying the pid
   * @param pid pid of the archival record
   * @return archival record entity
   */
  ArchivalRecord reserveArchivalRecordByPid(String pid);

  /**
   * Updates given archival record
   * @param archivalRecord dto containing data to change the archival record with
   * @return saved archival record entity
   */
  ArchivalRecord updateArchivalRecord(ArchivalRecordDto archivalRecord);


  /**
   * Updates an existing (reserved) archival record
   * @param pid id of the archival record
   * @param archivalRecordDraftDto contains data about archival record to be updated.
   */
  ArchivalRecord draftArchivalRecord(String pid, ArchivalRecordDraftDto archivalRecordDraftDto);

  /**
   * Publishes an existing, active archival record
   * @param pid if of the archival record
   * @param archivalRecordPublishDto contains data necessary for publishing process.
   */
  ArchivalRecord publishArchivalRecord(String pid, ArchivalRecordPublishDto archivalRecordPublishDto);

  /**
   * Should return the only active archival record for a digital object.
   * @param objectId id of the object
   * @return paged response of the active archival record.
   */
  PagedResponse<ArchivalRecordCompactView> findActiveArchivalRecordForObject(String objectId);


}
