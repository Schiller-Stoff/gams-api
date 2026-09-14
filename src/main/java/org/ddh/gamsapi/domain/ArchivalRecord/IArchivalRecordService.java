package org.ddh.gamsapi.domain.ArchivalRecord;

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


  ArchivalRecord createArchivalRecord(String objectId);

  void saveArchivalRecord(ArchivalRecordDto archivalRecord);


}
