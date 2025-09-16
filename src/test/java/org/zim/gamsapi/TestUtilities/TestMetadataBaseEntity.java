package org.zim.gamsapi.TestUtilities;

import org.zim.gamsapi.MetadataBaseEntity;
import org.zim.gamsapi.MetadataBaseEntityBuilder;

/**
 * Provides a test instance of MetadataBaseEntity
 * with hardcoded test values.
 */
public class TestMetadataBaseEntity {

  public static final String TITLE = "test-title";
  public static final String DESCRIPTION = "test-description";
  public static final String RIGHTS = "test-rights";
  public static final String CREATOR = "test-creator";
  public static final String MD5_CHECKSUM = "d41d8cd98f00b204e9800998ecf8427e";
  public static final String SHA512_CHECKSUM = "cf83e1357eefb8bdf1542850d66d8007d620e4050b5715dc83f4a921d14b2b0b822cd15d6c15b0f00a083e9b0c4b1d6f0b6bd6f0a2c4e6f7a5a6e0e5f8f5be5f";

  /**
   * Generates a MetadataBaseEntity with hardcoded test values.
   * @return MetadataBaseEntity with hardcoded test values.
   */
  public static MetadataBaseEntity generate(){
    return new MetadataBaseEntityBuilder()
        .title(TITLE)
        .rights(RIGHTS)
        .creator(CREATOR)
        .description(DESCRIPTION)
        .sha512Checksum(SHA512_CHECKSUM)
        .md5Checksum(MD5_CHECKSUM)
        .build();
  }

}
