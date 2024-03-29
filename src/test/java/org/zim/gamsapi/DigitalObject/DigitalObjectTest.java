package org.zim.gamsapi.DigitalObject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.zim.gamsapi.Datastream.Datastream;
import org.zim.gamsapi.UnitTest;
import org.zim.gamsapi.enums.TestDatastream;
import org.zim.gamsapi.enums.TestDigitalObject;

public class DigitalObjectTest extends UnitTest {



  @Test
  public void addDatastreamEstablishesBidirectionalRelationship(){

    Datastream datastream = new Datastream();
    datastream.setGlobalId(1L);

    DigitalObject digitalObject = new DigitalObject();
    digitalObject.setId(TestDigitalObject.DIGITAL_OBJECT_ID.getValue());

    digitalObject.addDatastream(datastream);

    // check equality
    Assertions.assertEquals(digitalObject, datastream.getDigitalObject());
    Assertions.assertEquals(digitalObject.getDatastreams().iterator().next(), datastream);

    // check if the datastream is in the digital object
    Assertions.assertTrue(digitalObject.getDatastreams().contains(datastream));

  }

  @Test
  public void digitalObjectsWithSameIdAreEqual(){
    DigitalObject digitalObject = new DigitalObject();
    digitalObject.setId(TestDigitalObject.DIGITAL_OBJECT_ID.getValue());
    DigitalObject digitalObject2 = new DigitalObject();
    digitalObject2.setId(TestDigitalObject.DIGITAL_OBJECT_ID.getValue());
    Assertions.assertEquals(digitalObject, digitalObject2);
  }

  @Test
  public void digitalObjectsWithDifferentIdAreNotEqual(){
    DigitalObject digitalObject = new DigitalObject();
    digitalObject.setId(TestDigitalObject.DIGITAL_OBJECT_ID.getValue());
    DigitalObject digitalObject2 = new DigitalObject();
    digitalObject2.setId("differentId");
    Assertions.assertNotEquals(digitalObject, digitalObject2);
  }

  @Test
  public void removeDatastreamRemovesBidirectionalRelationship(){

    Datastream datastream = new Datastream();
    datastream.setGlobalId(1L);

    DigitalObject digitalObject = new DigitalObject();
    digitalObject.setId(TestDigitalObject.DIGITAL_OBJECT_ID.getValue());

    digitalObject.addDatastream(datastream);

    // check equality
    Assertions.assertEquals(digitalObject, datastream.getDigitalObject());
    Assertions.assertEquals(digitalObject.getDatastreams().iterator().next(), datastream);

    //System.out.println("*****object: " + digitalObject);

    // check if the datastream is in the digital object
    Assertions.assertTrue(digitalObject.getDatastreams().contains(datastream));

    digitalObject.removeDatastream(datastream);

    // check if the datastream is removed from the digital object
    Assertions.assertFalse(digitalObject.getDatastreams().contains(datastream));
    Assertions.assertNull(datastream.getDigitalObject());

  }


}
