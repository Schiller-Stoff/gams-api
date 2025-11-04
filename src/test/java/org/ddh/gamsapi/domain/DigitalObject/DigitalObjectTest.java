package org.ddh.gamsapi.domain.DigitalObject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.ddh.gamsapi.UnitTest;
import org.ddh.gamsapi.TestUtilities.TestDigitalObject;

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
