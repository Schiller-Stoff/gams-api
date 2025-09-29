package org.zim.gamsapi.Ingest.utils.Bagit;


import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.TestInstance;
import org.zim.gamsapi.TestUtilities.TestBag;
import org.zim.gamsapi.UnitTest;

import java.io.IOException;

/**
 * TODO check BagTest
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class BagDirectoryReaderTest extends UnitTest {

  Bag bag;

  @BeforeAll
  public void setup() throws IOException {
    var testBag = TestBag.loadFile();
    bag = new Bag(testBag.toPath());
  }

  @Nested
  public class ReadSIPJson {



  }

}
