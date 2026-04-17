package org.ddh.gamsapi.domain.Datastream.utils;

/**
 * Controls whether a datastream is transferred to the research repository.
 * Overrides the default mimetype-based archival decision when explicitly set.
 */
public enum ArchivalPolicy {
  /**
   * Default mimetype-based procedure decides archival eligibility.
   * (e.g., images/XML archived, JSON not)
   */
  DEFAULT,

  /**
   * Force this datastream to be archived regardless of mimetype.
   */
  FORCE_ARCHIVE,

  /**
   * Force this datastream to NOT be archived regardless of mimetype.
   */
  FORCE_EXCLUDE
}