package org.ddh.gamsapi.domain.ArchivalRecord;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

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
  List<ArchivalRecordCompactView> findAllByDigitalObjectIdOrderByPublicationTimeStampDesc(String digitalObjectId);

  void deleteAllByDigitalObjectId(String digitalObjectId);


  boolean existsByDigitalObjectId(String digitalObjectId);

  /**
   * Sets all digital object references used from archival records to null.
   * @param digitalObjectId id of the digital object to be detached from archival records
   */
  @Transactional
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("UPDATE ArchivalRecord a SET a.digitalObject = NULL WHERE a.digitalObject.id = :digitalObjectId")
  void detachAllFromDigitalObject(@Param("digitalObjectId") String digitalObjectId);

}
