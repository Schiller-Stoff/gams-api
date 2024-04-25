package org.zim.gamsapi.Ingest.utils.Bagit;

/**
 * Represents the different files that are required to be present in a valid BagIt archive.
 */
public enum BagItFilePaths {

  /**
   * The bag-info.txt file is a tag file that contains metadata about the bag as a whole.
   */
  BAG_INFO_FILE_PATH("bag-info.txt"),

  /**
   * The bagit.txt file is a tag file that identifies the version of the BagIt specification that governs the structure of the bag.
   */
  BAG_TXT_FILE_PATH("bagit.txt"),

  /**
   * The data directory is a data directory that contains the files that comprise the content of the bag.
   */
  BAG_PAYLOAD_DIR_PATH("data"),

  /**
   * Path to the sip.json file.
   */
  BAG_SIP_JSON("data/meta/sip.json"),

  /**
   * Path to the bagit content directory.
   */
  BAG_CONTENT_DIR("data/content"),

  /**
   * Path to the bagit metadata directory.
   */
  BAG_METADATA_DIR("data/meta");


  public final String name;
  BagItFilePaths(String name){
    this.name = name;
  }

}
