package org.ddh.gamsapi.domain.DigitalObject;

import org.ddh.gamsapi.TestUtilities.TestDigitalObject;
import org.ddh.gamsapi.UnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class DigitalObjectIdTest extends UnitTest {

  @Test
  public void extractsExpectedProjectAbbr(){
    var objectId = new DigitalObjectId(TestDigitalObject.DIGITAL_OBJECT_ID.getValue());
    Assertions.assertEquals(objectId.deriveProjectAbbr(), TestDigitalObject.DIGITAL_OBJECT_PROJECT_ABBR.getValue());
  }

  @Test
  public void extractsFromComplexObjectId(){
    var objectId = new DigitalObjectId("demo.test.2.sd.1");
    Assertions.assertEquals("demo", objectId.deriveProjectAbbr());
  }

}
