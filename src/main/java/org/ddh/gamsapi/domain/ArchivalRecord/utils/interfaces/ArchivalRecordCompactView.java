package org.ddh.gamsapi.domain.ArchivalRecord.utils.interfaces;

import org.ddh.gamsapi.domain.ArchivalRecord.utils.ArchivalState;

import java.time.Instant;

/**
 * View for an ArchivalRecord summary.
 */
public interface ArchivalRecordCompactView {
  String getPid();
  String getDigitalObjectId();
  ArchivalState getArchivalState();
  String getExternalId();
  Instant getCreated();
  Instant getPublicationTimeStamp();
}
