package org.zim.gamsapi.TestUtilities;

import org.zim.gamsapi.domain.MetadataBaseEntity;
import org.zim.gamsapi.domain.MetadataBaseEntityBuilder;

/**
 * Provides a test instance of MetadataBaseEntity
 * with hardcoded test values.
 */
public class TestMetadataBaseEntity {

  public static final String TITLE = "test-title";
  public static final String DESCRIPTION = "test-description";
  public static final String RIGHTS = "test-rights";
  public static final String CREATOR = "test-creator";
  public static final String MD5_CHECKSUM = "540193d9633d8449ee1bff28030fe045";
  public static final String SHA512_CHECKSUM = "61eb68db4754a8349405f9355e86a72f32b00e17b747662c06c1c3027997d26d3cb1907e5f3ee3ec8299d67d97dc7c7ff4844dc70db8c5226666faf121540009";

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
