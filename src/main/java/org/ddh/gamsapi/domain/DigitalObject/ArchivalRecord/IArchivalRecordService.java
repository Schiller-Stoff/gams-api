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
   * Saves given ArchivalRecord.
   * @param archivalRecordCreateDto the ArchivalRecord to save
   * @return the saved ArchivalRecord
   */
  ArchivalRecord save(ArchivalRecordCreateDto archivalRecordCreateDto);

}
