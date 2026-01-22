package org.ddh.gamsapi.domain.DigitalObject.ArchivalRecord;

import java.time.Instant;

/**
 * View for an ArchivalRecord summary.
 */
public interface ArchivalRecordCompactView {
  Long getId();
  String getPid();
  Instant getTimeStamp();
}
