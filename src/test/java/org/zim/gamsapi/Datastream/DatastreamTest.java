package org.zim.gamsapi.Datastream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.zim.gamsapi.DigitalObject.DigitalObject;
import org.zim.gamsapi.DigitalObject.DigitalObjectBuilder;
import org.zim.gamsapi.MetadataBaseEntity;
import org.zim.gamsapi.MetadataBaseEntityBuilder;
import org.zim.gamsapi.UnitTest;
import org.zim.gamsapi.enums.TestDatastream;
import org.zim.gamsapi.enums.TestDigitalObject;
import org.zim.gamsapi.enums.TestMetadataBaseEntity;

public class DatastreamTest extends UnitTest {

  MetadataBaseEntity testMetadataBaseEntity = TestMetadataBaseEntity.generate();

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
          new DigitalObjectBuilder().id("FOO_BAR").project("foo").baseMetadata(testMetadataBaseEntity).build()
      );

      Datastream datastream2 = new Datastream();
      datastream2.setDsid(datastream.getDsid());
      datastream2.setDigitalObject(datastream.getDigitalObject());

      Assertions.assertEquals(datastream, datastream2);

    }


  }

  @Nested
  public class DeriveDatastreamId {

    @Test
    public void deriveDatastreamIdFromDigitalObjectAndDsidReturnsExpectedValues(){
      Datastream datastream = new Datastream();
      datastream.setDsid("dsid");
      datastream.setDigitalObject(
          new DigitalObjectBuilder().id("FOO_BAR").project("12345").baseMetadata(testMetadataBaseEntity).build()
      );

      DatastreamId datastreamId = datastream.deriveDatastreamId();
      Assertions.assertEquals(datastream.getDigitalObject().getId(), datastreamId.getDigitalObject());
      Assertions.assertEquals(datastream.getDsid(), datastreamId.getDsid());
    }

    @Test
    public void throwsExceptionWhenDigitalObjectIsNull(){
      Datastream datastream = new Datastream();
      datastream.setDsid("dsid");
      Assertions.assertThrows(
          IllegalStateException.class,
          datastream::deriveDatastreamId
      );
    }

    @Test
    public void throwsExceptionWhenDsidIsNull(){
      Datastream datastream = new Datastream();
      datastream.setDigitalObject(
          new DigitalObjectBuilder().id("FOO_BAR").project("foo").baseMetadata(testMetadataBaseEntity).build()
      );
      Assertions.assertThrows(
          IllegalStateException.class,
          datastream::deriveDatastreamId
      );
    }

  }



}
