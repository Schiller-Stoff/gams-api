package org.ddh.gamsapi.domain.ArchivalRecord.utils.interfaces;

import java.time.Instant;

/**
 * Lightweight scalar projection of a DigitalObject for archival status checks.
 * Avoids instantiating DigitalObject entity (prevents EAGER fetching of tags).
 */
public interface DigitalObjectArchivalBaseView {
  String getObjectId();
  String getProjectAbbr();
  Instant getModified();
}