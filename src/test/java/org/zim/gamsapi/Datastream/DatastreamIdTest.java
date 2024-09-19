package org.zim.gamsapi.Datastream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.zim.gamsapi.UnitTest;

public class DatastreamIdTest extends UnitTest {

  @Test
  public void testToString() {
    DatastreamId datastreamId = new DatastreamId("dsid", "digitalObject");
    Assertions.assertEquals("digitalObject_dsid", datastreamId.toString());
  }

}
