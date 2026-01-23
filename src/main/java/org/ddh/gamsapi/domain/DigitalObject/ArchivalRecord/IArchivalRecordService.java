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
   * Saves given ArchivalRecord.
   * @param archivalRecordCreateDto the ArchivalRecord to save
   * @return the saved ArchivalRecord
   */
  ArchivalRecord save(ArchivalRecordCreateDto archivalRecordCreateDto);

}
