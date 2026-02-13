package org.ddh.gamsapi.domain.DigitalObject.utils;

/**
 * Represents if a digital object was stored in a research repository,
 */
public enum ArchiveState {
  /**
   * The digital object was never archived in a research repository
   */
  NOT_ARCHIVED,

  /**
   * The current state of the digital object is fully represented in a research repository.
   */
  ARCHIVED,

  /**
   * The current state of the digital object is not fully represented (outdated) in a research repository.
   */
  REARCHIVING_REQUIRED;
}
