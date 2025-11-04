package org.ddh.gamsapi.domain.Datastream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.ddh.gamsapi.UnitTest;

public class DatastreamIdTest extends UnitTest {

  @Test
  public void testToString() {
    DatastreamId datastreamId = new DatastreamId("dsid", "digitalObject");
    Assertions.assertEquals("digitalObject_dsid", datastreamId.toString());
  }

}
