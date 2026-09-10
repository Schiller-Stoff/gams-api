package org.ddh.gamsapi.domain.DigitalObject.ArchivalRecord;

import org.springframework.data.repository.CrudRepository;

import java.util.Collection;
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
  List<ArchivalRecordCompactView> findAllByDigitalObjectIdOrderByPublicationTimeStampDesc(String digitalObjectId);

  void deleteAllByDigitalObjectId(String digitalObjectId);


  boolean existsByDigitalObjectId(String digitalObjectId);

}
