package org.ddh.gamsapi.TestUtilities;

import org.ddh.gamsapi.domain.MetadataBaseEntity;
import org.ddh.gamsapi.domain.MetadataBaseEntityBuilder;

/**
 * Provides a test instance of MetadataBaseEntity
 * with hardcoded test values.
 */
public class TestMetadataBaseEntity {

  public static final String TITLE = "test-title";
  public static final String DESCRIPTION = "test-description";
  public static final String RIGHTS = "test-rights";
  public static final String CREATOR = "test-creator";

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
        .build();
  }

}
