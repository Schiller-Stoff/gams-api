package org.ddh.gamsapi.domain.DigitalObject.ArchivalRecord;

import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for managing ArchivalRecord entities.
 */
public interface IArchivalRecordRepository extends CrudRepository<ArchivalRecord, Long> {

  /**
   * Find ArchivalRecord by DigitalObject ID.
   * @param digitalObjectId the ID of the associated DigitalObject
   * @return the ArchivalRecord
   */
  List<ArchivalRecordCompactView> findAllByDigitalObjectIdOrderByTimeStampDesc(String digitalObjectId);

  void deleteAllByDigitalObjectId(String digitalObjectId);


  List<ArchivalRecordCompactView> findArchivalRecordsByDigitalObjectIdAndArchivingStatus(String digitalObjectId, ArchivingStatus archivingStatus);

  boolean existsByDigitalObjectId(String digitalObjectId);

  Optional<ArchivalRecordCompactView> findByArchivingStatus(ArchivingStatus archivingStatus);

  boolean existsByArchivingStatus(ArchivingStatus archivingStatus);

}
