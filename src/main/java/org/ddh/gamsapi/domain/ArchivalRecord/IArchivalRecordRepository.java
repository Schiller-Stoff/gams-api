package org.ddh.gamsapi.domain.ArchivalRecord;

import jakarta.persistence.LockModeType;
import org.ddh.gamsapi.domain.ArchivalRecord.utils.ArchivalState;
import org.ddh.gamsapi.domain.ArchivalRecord.utils.interfaces.DigitalObjectArchivalBaseView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

  /**
   * TODO jdoc
   * @param objectIds
   * @return
   */
  List<ArchivalRecordCompactView> findAllByDigitalObjectIdInOrderByCreatedDesc(List<String> objectIds);

  /**
   * Paginated query over DigitalObject returning ONLY scalar columns.
   *
   * Logic for "needsArchiving = true":
   * A DigitalObject needs archiving attention if:
   * - It has an active/blocking record (RESERVED or DRAFT), OR
   * - It does NOT have a PUBLISHED record whose publicationTimeStamp >= d.modified
   *   (This single NOT EXISTS covers BOTH:
   *     1. Objects with zero archival records at all, AND
   *     2. Objects modified after their latest PUBLISHED record!)
   */
  @Query(
      value = """
          SELECT 
              d.id AS objectId,
              d.project.projectAbbr AS projectAbbr,
              d.modified AS modified
          FROM DigitalObject d
          WHERE (:projectAbbr IS NULL OR d.project.projectAbbr = :projectAbbr)
            AND (:idPrefix IS NULL OR d.id LIKE CONCAT(:idPrefix, '%'))
            AND (
                :needsArchiving = false
                OR EXISTS (
                    SELECT 1 FROM ArchivalRecord arActive
                    WHERE arActive.digitalObjectId = d.id
                      AND arActive.archivalState IN (
                          org.ddh.gamsapi.domain.ArchivalRecord.utils.ArchivalState.RESERVED,
                          org.ddh.gamsapi.domain.ArchivalRecord.utils.ArchivalState.DRAFT
                      )
                )
                OR NOT EXISTS (
                    SELECT 1 FROM ArchivalRecord arPub
                    WHERE arPub.digitalObjectId = d.id
                      AND arPub.archivalState = org.ddh.gamsapi.domain.ArchivalRecord.utils.ArchivalState.PUBLISHED
                      AND arPub.publicationTimeStamp >= d.modified
                )
            )
          """,
      countQuery = """
          SELECT COUNT(d.id)
          FROM DigitalObject d
          WHERE (:projectAbbr IS NULL OR d.project.projectAbbr = :projectAbbr)
            AND (:idPrefix IS NULL OR d.id LIKE CONCAT(:idPrefix, '%'))
            AND (
                :needsArchiving = false
                OR EXISTS (
                    SELECT 1 FROM ArchivalRecord arActive
                    WHERE arActive.digitalObjectId = d.id
                      AND arActive.archivalState IN (
                          org.ddh.gamsapi.domain.ArchivalRecord.utils.ArchivalState.RESERVED,
                          org.ddh.gamsapi.domain.ArchivalRecord.utils.ArchivalState.DRAFT
                      )
                )
                OR NOT EXISTS (
                    SELECT 1 FROM ArchivalRecord arPub
                    WHERE arPub.digitalObjectId = d.id
                      AND arPub.archivalState = org.ddh.gamsapi.domain.ArchivalRecord.utils.ArchivalState.PUBLISHED
                      AND arPub.publicationTimeStamp >= d.modified
                )
            )
          """
  )
  Page<DigitalObjectArchivalBaseView> findDigitalObjectsForArchivalCheck(
      @Param("projectAbbr") String projectAbbr,
      @Param("needsArchiving") boolean needsArchiving,
      @Param("idPrefix") String idPrefix,
      Pageable pageable
  );
}
