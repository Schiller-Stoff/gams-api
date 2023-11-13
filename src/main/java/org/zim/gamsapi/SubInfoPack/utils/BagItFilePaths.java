package org.zim.gamsapi.SubInfoPack.utils;

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
  BAG_PAYLOAD_DIR_PATH("data");


  public final String name;
  BagItFilePaths(String name){
    this.name = name;
  }

}
