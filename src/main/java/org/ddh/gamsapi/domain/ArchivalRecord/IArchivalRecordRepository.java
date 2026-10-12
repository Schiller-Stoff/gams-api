package org.ddh.gamsapi.domain.ArchivalRecord;

import jakarta.persistence.LockModeType;
import org.ddh.gamsapi.domain.ArchivalRecord.utils.ArchivalState;
import org.ddh.gamsapi.domain.ArchivalRecord.utils.dto.ArchivalRecordDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
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
public interface IArchivalRecordRepository extends JpaRepository<ArchivalRecord, String> {

  /**
   * Find ArchivalRecord by DigitalObject ID.
   * @param digitalObjectId the ID of the associated DigitalObject
   * @return the ArchivalRecord
   */
  Page<ArchivalRecordCompactView> findAllByDigitalObjectIdOrderByPublicationTimeStampDesc(String digitalObjectId, Pageable pageable);

  void deleteAllByDigitalObjectId(String digitalObjectId);

  boolean existsByDigitalObjectId(String digitalObjectId);

  boolean existsByDigitalObjectIdAndArchivalState(String digitalObjectId, ArchivalState archivalState);

  boolean existsByDigitalObjectIdAndArchivalStateIn(String digitalObjectId, Collection<ArchivalState> archivalStates);

  List<ArchivalRecord> findByDigitalObjectIdAndArchivalStateIn(String digitalObjectId, Collection<ArchivalState> archivalStates);

  Optional<ArchivalRecordCompactView> findActiveByDigitalObjectIdAndArchivalStateIn(String digitalObjectId, Collection<ArchivalState> archivalStates);

  Page<ArchivalRecordCompactView> findActiveArchivalRecordsByDigitalObjectIdAndArchivalStateIn(String digitalObjectId, Collection<ArchivalState> archivalStates, Pageable  pageable);

  Page<ArchivalRecordCompactView> findAllByDigitalObjectIdAndArchivalStateIn(String digitalObjectId, Collection<ArchivalState> archivalStates, Pageable pageable);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT a FROM ArchivalRecord a WHERE a.pid = :pid")   // adapt if the @Id field isn't named pid
  Optional<ArchivalRecord> findByIdForUpdate(@Param("pid") String pid);

}
