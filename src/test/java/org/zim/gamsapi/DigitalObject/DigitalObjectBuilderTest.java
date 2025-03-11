package org.zim.gamsapi.DigitalObject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.zim.gamsapi.MetadataBaseEntity;
import org.zim.gamsapi.UnitTest;
import org.zim.gamsapi.enums.TestMetadataBaseEntity;

import java.util.Set;

public class DigitalObjectBuilderTest extends UnitTest {


  MetadataBaseEntity testMetadataBaseEntity = TestMetadataBaseEntity.generate();

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
  public void throwsIfProjectIsNotSet() {
    Assertions.assertThrows(IllegalStateException.class, () -> {
      new DigitalObjectBuilder().id("12345").build();
    });
  }

  @Test
  public void throwsIfProjectIsEmpty(){
    Assertions.assertThrows(IllegalStateException.class, () -> {
      new DigitalObjectBuilder()
          .id("12345")
          .publisher("test-publisher")
          .project("")
          .baseMetadata(testMetadataBaseEntity)
          .build();
    });
  }

  @Test
  public void throwsIfPublisherIsNotSet(){
    Assertions.assertThrows(IllegalStateException.class, () -> {
      new DigitalObjectBuilder()
          .id("12345")
          .project("projectAbbr")
          .baseMetadata(testMetadataBaseEntity)
          .build();
    });
  }

  @Test
  public void mayBuildADigitalObjectWithExpectedValues(){
    DigitalObject digitalObject = new DigitalObjectBuilder()
        .id("1")
        .objectType("type")
        .project("projectAbbr")
        .publisher("test-publisher")
        .baseMetadata(testMetadataBaseEntity)
        .build();

    Assertions.assertEquals("1", digitalObject.getId());
    Assertions.assertEquals("type", digitalObject.getObjectType());
    Assertions.assertEquals("test-title", digitalObject.getBaseMetadata().getTitle());
  }


}
