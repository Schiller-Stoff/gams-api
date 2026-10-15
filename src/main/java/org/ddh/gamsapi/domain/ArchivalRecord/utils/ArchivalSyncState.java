package org.ddh.gamsapi.domain.ArchivalRecord.utils;

/**
 * Deterministic state telling the client whether archiving is needed
 * and what exact follow-up action to take.
 */
public enum ArchivalSyncState {

  /**
   * Rule 1: DigitalObject has no ArchivalRecords at all.
   * Client action: POST new ArchivalRecord (RESERVED/DRAFT).
   */
  NEVER_ARCHIVED,

  /**
   * Rule 3a: DigitalObject has an active record (RESERVED or DRAFT)
   * created AFTER or AT the last DigitalObject modification.
   * Client action: Continue/finish publishing the active record (via pid/externalId).
   */
  ARCHIVING_IN_PROGRESS,

  /**
   * Rule 3b (Edge Case!): DigitalObject has an active record (RESERVED or DRAFT),
   * BUT the DigitalObject was modified AGAIN after the active record was created.
   * Client action: Update the external RDM deposit (externalId) or recreate the draft before publishing.
   */
  ACTIVE_RECORD_OUTDATED,

  /**
   * Rule 2: No active record exists, and DigitalObject.modified > latest PUBLISHED publicationTimeStamp.
   * Client action: POST a new ArchivalRecord for re-archiving.
   */
  NEEDS_REARCHIVING,

  /**
   * DigitalObject has a PUBLISHED record with publicationTimeStamp >= DigitalObject.modified,
   * and no active record exists.
   * Client action: None (Up to date).
   */
  UP_TO_DATE
}