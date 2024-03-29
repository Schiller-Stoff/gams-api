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
    public void datastreamsWithSameDsidAreNotEqual(){
      Datastream datastream = new Datastream();
      datastream.setDsid(TestDatastream.DSID.getValue());
      Datastream datastream2 = new Datastream();
      datastream2.setDsid(TestDatastream.DSID.getValue());
      Assertions.assertNotEquals(datastream, datastream2);
    }

    @Test
    public void datastreamsWithSameGlobalIdAreEqual(){
      Datastream datastream = new Datastream();
      datastream.setGlobalId(1L);
      Datastream datastream2 = new Datastream();
      datastream2.setGlobalId(1L);
      Assertions.assertEquals(datastream, datastream2);
    }

    @Test
    public void datastreamsWithDifferentGlobalIdAreNotEqual(){
      Datastream datastream = new Datastream();
      datastream.setGlobalId(1L);
      Datastream datastream2 = new Datastream();
      datastream2.setGlobalId(2L);
      Assertions.assertNotEquals(datastream, datastream2);
    }

    @Test
    public void datastreamsWithSamePidAndDsidAreNotEqualIfDifferentGlobalId(){
      Datastream datastream = new Datastream();
      DigitalObject digitalObject = new DigitalObject();
      digitalObject.setId(TestDigitalObject.DIGITAL_OBJECT_ID.getValue());
      datastream.setDigitalObject(digitalObject);
      datastream.setDsid(TestDatastream.DSID.getValue());
      Datastream datastream2 = new Datastream();
      datastream2.setDsid(TestDatastream.DSID.getValue());
      datastream2.setDigitalObject(digitalObject);
      Assertions.assertNotEquals(datastream, datastream2);
    }

  }



}
