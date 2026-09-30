package org.ddh.gamsapi.domain.ArchivalRecord;

import org.ddh.gamsapi.domain.ArchivalRecord.utils.ArchivalState;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for managing ArchivalRecord entities.
 */
public interface IArchivalRecordRepository extends CrudRepository<ArchivalRecord, String> {

  /**
   * Find ArchivalRecord by DigitalObject ID.
   * @param digitalObjectId the ID of the associated DigitalObject
   * @return the ArchivalRecord
   */
  Page<ArchivalRecordCompactView> findAllByDigitalObjectIdOrderByPublicationTimeStampDesc(String digitalObjectId, Pageable pageable);

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

  boolean existsByDigitalObjectIdAndArchivalState(String digitalObjectId, ArchivalState archivalState);

  boolean existsByDigitalObjectIdAndArchivalStateIn(String digitalObjectId, Collection<ArchivalState> archivalStates);

  List<ArchivalRecord> findByDigitalObjectIdAndArchivalStateIn(String digitalObjectId, Collection<ArchivalState> archivalStates);

  Optional<ArchivalRecordCompactView> findActiveByDigitalObjectIdAndArchivalStateIn(String digitalObjectId, Collection<ArchivalState> archivalStates);

  Page<ArchivalRecordCompactView> findActiveArchivalRecordsByDigitalObjectIdAndArchivalStateIn(String digitalObjectId, Collection<ArchivalState> archivalStates, Pageable  pageable);

  Page<ArchivalRecordCompactView> findAllByDigitalObjectIdAndArchivalStateIn(String digitalObjectId, Collection<ArchivalState> archivalStates, Pageable pageable);

}
