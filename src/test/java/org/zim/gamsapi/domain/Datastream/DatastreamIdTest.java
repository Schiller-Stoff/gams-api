package org.zim.gamsapi.domain.Datastream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.zim.gamsapi.UnitTest;
import org.zim.gamsapi.domain.Datastream.DatastreamId;

public class DatastreamIdTest extends UnitTest {

  @Test
  public void testToString() {
    DatastreamId datastreamId = new DatastreamId("dsid", "digitalObject");
    Assertions.assertEquals("digitalObject_dsid", datastreamId.toString());
  }

}
