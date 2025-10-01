package org.zim.gamsapi.DigitalObject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.zim.gamsapi.MetadataBaseEntity;
import org.zim.gamsapi.TestUtilities.TestDigitalObject;
import org.zim.gamsapi.UnitTest;
import org.zim.gamsapi.TestUtilities.TestMetadataBaseEntity;

import java.util.Date;

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

  @Test
  public void mayBuildADigitalObjectWithExpectedValuesAndFunder(){
    DigitalObject digitalObject = new DigitalObjectBuilder()
        .id("projectAbbr.1")
        .project("projectAbbr")
        .publisher("test-publisher")
        .funder("test-funder")
        .build();

    Assertions.assertEquals("test-funder", digitalObject.getFunder());
  }

  @Test
  public void builderHelperMethodReturnsAnInstance(){
    DigitalObjectBuilder digitalObjectBuilder = DigitalObjectBuilder.builder();
    Assertions.assertNotNull(digitalObjectBuilder);
    Assertions.assertTrue(digitalObjectBuilder instanceof DigitalObjectBuilder);

  }

  @Test
  public void mayBuildDigitalObjectWithExpectedMainResource(){
    final String MAIN_RESOURCE = "mainResource";

    DigitalObject digitalObject = new DigitalObjectBuilder()
        .id("projectAbbr.1")
        .project("projectAbbr")
        .publisher("test-publisher")
        .funder("test-funder")
        .mainResource(MAIN_RESOURCE)
        .build();

    Assertions.assertEquals(MAIN_RESOURCE, digitalObject.getMainResource());

  }

  @Test
  public void buildsDigitalObjectWithExpectedNullValues(){
      var digitalObject = new DigitalObjectBuilder()
              .id(TestDigitalObject.DIGITAL_OBJECT_ID.getValue())
              .project(TestDigitalObject.DIGITAL_OBJECT_PROJECT_ABBR.getValue())
              .publisher(TestDigitalObject.DIGITAL_OBJECT_PUBLISHER.getValue())
              .baseMetadata(testMetadataBaseEntity)
              .objectType(TestDigitalObject.DIGITAL_OBJECT_ID.getValue())
              .funder(TestDigitalObject.DIGITAL_OBJECT_FUNDER.getValue())
              .mainResource(TestDigitalObject.DIGITAL_OBJECT_MAIN_RESOURCE.getValue())
              .published(new Date())
              .bagSource(TestDigitalObject.DIGITAL_OBJECT_BAG_SOURCE.getValue())
              .bagSchema(TestDigitalObject.DIGITAL_OBJECT_BAG_SCHEMA.getValue())
              .bagCreatedBy(TestDigitalObject.DIGITAL_OBJECT_BAG_SCHEMA.getValue())
              .build();

      org.assertj.core.api.Assertions.assertThat(digitalObject).hasNoNullFieldsOrPropertiesExcept(
              "created", "modified", "createdBy", "modifiedBy"
      );

  }

}
