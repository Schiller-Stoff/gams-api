package org.zim.gamsapi.enums;

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


}
