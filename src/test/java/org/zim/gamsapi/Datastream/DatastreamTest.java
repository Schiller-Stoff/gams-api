package org.zim.gamsapi.Datastream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.zim.gamsapi.DigitalObject.DigitalObject;
import org.zim.gamsapi.UnitTest;
import org.zim.gamsapi.enums.TestDatastream;
import org.zim.gamsapi.enums.TestDigitalObject;

public class DatastreamTest extends UnitTest {


  @Nested
  public class IdentityTests {

    @Test
    public void comparingDatastreamsWithJustSameDsidThrows(){
      Datastream datastream = new Datastream();
      datastream.setDsid(TestDatastream.DSID.getValue());
      Datastream datastream2 = new Datastream();
      datastream2.setDsid(TestDatastream.DSID.getValue());

      Assertions.assertThrows(
          IllegalStateException.class,
          () -> Assertions.assertEquals(datastream, datastream2)
      );
    }

    @Test
    public void datastreamsWithSamdeDsidAndDigitalObjectAreEqual(){
      Datastream datastream = new Datastream();
      datastream.setDsid("dsid");
      datastream.setDigitalObject(
          DigitalObject.builder().id("FOO_BAR").build()
      );

      Datastream datastream2 = new Datastream();
      datastream2.setDsid(datastream.getDsid());
      datastream2.setDigitalObject(datastream.getDigitalObject());

      Assertions.assertEquals(datastream, datastream2);

    }


  }

  @Nested
  public class DeriveDatastreamId {

    // TODO include some tests here

  }



}
