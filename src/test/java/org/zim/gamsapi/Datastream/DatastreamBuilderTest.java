package org.zim.gamsapi.Datastream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.zim.gamsapi.UnitTest;

public class DatastreamBuilderTest extends UnitTest {

  @Test
  public void throwsIfDigitalObjectIsNull(){
    Assertions.assertThrows(IllegalStateException.class, () -> {
      new DatastreamBuilder()
          .dsid("dsid")
          .build();
    });
  }

  public void throwsIfDigitalObjectIdIsEmpty(){
    Assertions.assertThrows(IllegalStateException.class, () -> {
      new DatastreamBuilder()
          .digitalObject("")
          .dsid("dsid")
          .build();
    });
  }

  @Test
  public void throwsIfDsidIsNull(){
    Assertions.assertThrows(IllegalStateException.class, () -> {
      new DatastreamBuilder()
          .digitalObject("digitalObjectId")
          .build();
    });
  }

  @Test
  public void throwsIfDsidIsEmpty(){
    Assertions.assertThrows(IllegalStateException.class, () -> {
      new DatastreamBuilder()
          .digitalObject("digitalObjectId")
          .dsid("")
          .build();
    });
  }

  @Test
  public void mayBuildDatastream(){
    Datastream datastream = new DatastreamBuilder()
        .digitalObject("digitalObjectId")
        .dsid("dsid")
        .build();

    Assertions.assertNotNull(datastream);
    Assertions.assertEquals("digitalObjectId", datastream.getDigitalObject().getId());
    Assertions.assertEquals("dsid", datastream.getDsid());
  }

}
