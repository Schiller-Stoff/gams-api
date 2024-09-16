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
          .project("")
          .baseMetadata(testMetadataBaseEntity)
          .build();
    });
  }

  @Test
  public void mayBuildADigitalObjectWithExpectedValues(){
    DigitalObject digitalObject = new DigitalObjectBuilder()
        .id("1")
        .objectType("type")
        .types(Set.of("type"))
        .project("projectAbbr")
        .baseMetadata(testMetadataBaseEntity)
        .build();

    Assertions.assertEquals("1", digitalObject.getId());
    Assertions.assertEquals("type", digitalObject.getObjectType());
    Assertions.assertEquals(Set.of("type"), digitalObject.getTypes());
    Assertions.assertEquals("test-title", digitalObject.getBaseMetadata().getTitle());
  }

  @Test
  public void parentObjectHasSameProjectAssigned(){

    final String TEST_PROJECT_ABBR = "testProjectAbbr";

    DigitalObject digitalObject = new DigitalObjectBuilder()
        .id("1")
        .project(TEST_PROJECT_ABBR)
        .parent("2")
        .baseMetadata(testMetadataBaseEntity)
        .build();

    Assertions.assertEquals(
        digitalObject.getParent().getProject().getProjectAbbr(), TEST_PROJECT_ABBR
    );

  }

  @Test
  public void allowsTosetParentObjectViaPidAsString_hasSameId(){
    DigitalObject digitalObject = new DigitalObjectBuilder()
        .id("1")
        .project("projectAbbr")
        .parent("2")
        .baseMetadata(testMetadataBaseEntity)
        .build();

    Assertions.assertEquals("2", digitalObject.getParent().getId());
  }


}
