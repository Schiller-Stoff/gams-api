package org.ddh.gamsapi.domain.ArchivalRecord;

import org.ddh.gamsapi.domain.ArchivalRecord.utils.ArchivalState;

import java.time.Instant;

/**
 * View for an ArchivalRecord summary.
 */
public interface ArchivalRecordCompactView {
  String getPid();
  Instant getPublicationTimeStamp();
  String getExternalId();
  ArchivalState getArchivalState();
}
