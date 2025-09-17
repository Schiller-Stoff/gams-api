package org.zim.gamsapi.TestUtilities;

import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.io.IOException;

/**
 * Utility class for loading test bag files.
 */
public class TestBag {

  public static final String FOLDER_LOCATION = "testfiles/ingest/test-bag";

  private TestBag() {
    throw new IllegalStateException("Utility class");
  }

  public static File loadFile() throws IOException {
    return new ClassPathResource(FOLDER_LOCATION).getFile();
  }

  public static class TestBagInfo {
    public static final String BAGGING_DATE = "2025-08-19";
    public static final String BAGGING_TIME = "12:07:07 UTC";
    public static final String PAYLOAD_OXUM = "1140704.5";
    public static final String CONTACT_EMAIL = "dh@uni-graz.at";
    public static final String EXTERNAL_DESCRIPTION = "Test bag for gamsapi tests";
  }
}
