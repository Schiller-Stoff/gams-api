package org.ddh.gamsapi.domain.DigitalObject.ArchivalRecord;

import org.springframework.data.repository.CrudRepository;

import java.util.List;

/**
 * Repository interface for managing ArchivalRecord entities.
 */
public interface IArchivalRecordRepository extends CrudRepository<ArchivalRecord, String> {

  /**
   * Find ArchivalRecord by DigitalObject ID.
   * @param digitalObjectId the ID of the associated DigitalObject
   * @return the ArchivalRecord
   */
  List<ArchivalRecordCompactView> findAllByDigitalObjectIdOrderByTimeStampDesc(String digitalObjectId);

  void deleteAllByDigitalObjectId(String digitalObjectId);

}
