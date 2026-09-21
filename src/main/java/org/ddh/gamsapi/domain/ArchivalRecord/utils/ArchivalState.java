package org.ddh.gamsapi.domain.ArchivalRecord.utils;

import java.util.Collection;
import java.util.EnumSet;

/**
 * Represents the status of archiving of given archival record.
 */
public enum ArchivalState {

  /**
   * Archiving info is provided to the gams-api BUT the external archiving process is not finished yet.
   * E.g. permanent identifier is assigned to gams-api but not available to public at the moment.
   */
  RESERVED,

  /**
   * Archiving is drafted but not publicly available.
   * Stored in RDM repository but not finished external pid assignment.
   */
  DRAFT,

  /**
   * Archival record is completely public.
   */
  PUBLISHED;

  /**
   * Returns the blocking enum statuses of a digital object.
   * Blocking means: there can only be one ArchivalRecord that is in reserved state.
   * @return collection of blocking statuses
   */
  public static Collection<ArchivalState> getBlockingStatuses(){
    return EnumSet.of(ArchivalState.RESERVED, ArchivalState.DRAFT);
  }

}
