package org.zim.gamsapi.Ingest;

import lombok.Data;
import lombok.ToString;

/**
 * Domain class representing an ingest operation
 *
 */
@Data
@ToString
public class Ingest {

  /**
   * Abbreviation of the project the ingest operation belongs to
   * (usually provided by path variable)
   * TODO add validation?
   */
  private String projectAbbr;

  /**
   * The zipped bag.
   * TODO add validation?
   */
  @ToString.Exclude
  private byte[] zippedBagItFolder;

}
