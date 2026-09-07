package org.ddh.gamsapi.domain.DigitalObject.ArchivalRecord;

/**
 * Represents the status of the archiving process of digital objects.
 */
public enum ArchivingStatus {

  /**
   * Archiving info is provided to the gams-api BUT the external archiving process is not finished yet.
   * E.g. permanent identifier is assigned to gams-api but not available to public at the moment.
   */
  DRAFTED,

  /**
   * Archiving is finished but not publicly available.
   * Stored in RDM repository but not finished external pid assignment.
   */
  ARCHIVED,

  /**
   * Archival record is completely public.
   */
  PUBLISHED
}
