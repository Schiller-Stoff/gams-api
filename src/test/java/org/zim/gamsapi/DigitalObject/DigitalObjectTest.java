package org.zim.gamsapi.DigitalObject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.zim.gamsapi.Datastream.Datastream;
import org.zim.gamsapi.UnitTest;
import org.zim.gamsapi.enums.TestDatastream;
import org.zim.gamsapi.enums.TestDigitalObject;

public class DigitalObjectTest extends UnitTest {


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



}
