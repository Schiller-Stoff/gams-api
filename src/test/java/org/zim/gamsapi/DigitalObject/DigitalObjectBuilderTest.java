package org.zim.gamsapi.DigitalObject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.zim.gamsapi.MetadataBaseEntity;
import org.zim.gamsapi.UnitTest;

import java.util.Set;

public class DigitalObjectBuilderTest extends UnitTest {

  @Test
  public void throwsIfIdIsNotSet(){
    Assertions.assertThrows(IllegalStateException.class, () -> {
      new DigitalObjectBuilder().build();
    });
  }

  @Test
  public void throwsIfIdIsEmpty(){
    Assertions.assertThrows(IllegalStateException.class, () -> {
      new DigitalObjectBuilder().id("").build();
    });
  }

  @Test
  public void mayBuildADigitalObjectWithExpectedValues(){
    DigitalObject digitalObject = new DigitalObjectBuilder()
        .id("1")
        .objectType("type")
        .types(Set.of("type"))
        .baseMetadata(MetadataBaseEntity.builder().title("test-title").build())
        .build();

    Assertions.assertEquals("1", digitalObject.getId());
    Assertions.assertEquals("type", digitalObject.getObjectType());
    Assertions.assertEquals(Set.of("type"), digitalObject.getTypes());
    Assertions.assertEquals("test-title", digitalObject.getBaseMetadata().getTitle());
  }


}
