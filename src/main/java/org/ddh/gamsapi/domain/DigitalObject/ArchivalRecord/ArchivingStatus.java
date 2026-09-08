package org.ddh.gamsapi.domain.DigitalObject.ArchivalRecord;

import java.util.Collection;
import java.util.EnumSet;

/**
 * Represents the status of the archiving process of digital objects.
 */
public enum ArchivingStatus {

  /**
   * Archiving info is provided to the gams-api BUT the external archiving process is not finished yet.
   * E.g. permanent identifier is assigned to gams-api but not available to public at the moment.
   */
  RESERVED,

  /**
   * Archiving is drafted but not publicly available.
   * Stored in RDM repository but not finished external pid assignment.
   */
  DRAFTED,

  /**
   * Archival record is completely public.
   */
  PUBLISHED;

  /**
   * Returns the blocking enum statuses of a digital object.
   * Blocking means: there can only be one ArchivalRecord that is in reserved state.
   * @return collection of blocking statuses
   */
  static Collection<ArchivingStatus> gtBlockingStatuses(){
    return EnumSet.of(ArchivingStatus.RESERVED, ArchivingStatus.DRAFTED);
  }
}
