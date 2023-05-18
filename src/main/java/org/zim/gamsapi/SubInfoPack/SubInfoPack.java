package org.zim.gamsapi.SubInfoPack;

import lombok.Data;
import lombok.ToString;

/**
 * Domain class representing a Submission Information Package
 *
 */
@Data
public class SubInfoPack {

  // contained in metadata
  //private String pid;

  // TODO use for validation?
  private String projectAbbr;

  // should be contained in metadata
  // private String type;

  /**
   * Represents the profile of the ingest process.
   * Like "simple" - "basic"
   * TODO use enum here / look up how to combine with database / JPA / hibernate
   */
  private String ingestProfile;

  @ToString.Exclude
  private byte[] zippedFolder;


  //TODO add foldername for easier logging?

}
