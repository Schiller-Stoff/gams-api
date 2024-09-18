package org.zim.gamsapi.enums;

import org.zim.gamsapi.MetadataBaseEntity;
import org.zim.gamsapi.MetadataBaseEntityBuilder;

/**
 * Provides a test instance of MetadataBaseEntity
 * with hardcoded test values.
 */
public class TestMetadataBaseEntity {

  /**
   * Generates a MetadataBaseEntity with hardcoded test values.
   * @return MetadataBaseEntity with hardcoded test values.
   */
  public static MetadataBaseEntity generate(){
    return new MetadataBaseEntityBuilder()
        .title("test-title")
        .rights("test-rights")
        .creator("test-creator")
        .description("test-description")
        .build();
  }

}
