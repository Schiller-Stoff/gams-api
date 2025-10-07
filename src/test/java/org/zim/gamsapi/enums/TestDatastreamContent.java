package org.zim.gamsapi.enums;

import org.springframework.mock.web.MockMultipartFile;

/**
 * Enum for test datastream content.
 */
public enum TestDatastreamContent {

  NAME("test-file"),

  ORIGINAL_FILENAME("test.txt"),

  CONTENT_TYPE("text/plain"),

  CONTENT("test-content");

  private final String value;

  TestDatastreamContent(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }

  /**
   * Generates a test datastream content using the values defined in the enum.
   * @return The generated datastream content.
   */
  public static MockMultipartFile generate(){
    return new MockMultipartFile(NAME.getValue(), ORIGINAL_FILENAME.getValue(), CONTENT_TYPE.getValue(), CONTENT.getValue().getBytes());
  }


}
