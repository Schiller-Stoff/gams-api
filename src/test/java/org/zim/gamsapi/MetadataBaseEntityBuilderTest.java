package org.zim.gamsapi;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class MetadataBaseEntityBuilderTest extends UnitTest {

  @Test
  public void throwsIfTitleIsNotSet(){
    Assertions.assertThrows(IllegalStateException.class, () -> {
      new MetadataBaseEntityBuilder().build();
    });
  }

  @Test
  public void throwsIfTitleIsEmpty(){
    Assertions.assertThrows(IllegalStateException.class, () -> {
      new MetadataBaseEntityBuilder().title("").build();
    });
  }

  @Test
  public void throwsIfRightsAreNotSet(){
    Assertions.assertThrows(IllegalStateException.class, () -> {
      new MetadataBaseEntityBuilder().title("test-title").build();
    });
  }

  @Test
  public void throwsIfRightsAreEmpty(){
    Assertions.assertThrows(IllegalStateException.class, () -> {
      new MetadataBaseEntityBuilder().title("test-title").rights("").build();
    });
  }

  @Test
  public void throwsIfPublisherIsNotSet(){
    Assertions.assertThrows(IllegalStateException.class, () -> {
      new MetadataBaseEntityBuilder().title("test-title").rights("test-rights").build();
    });
  }

  @Test
  public void throwsIfCreatorIsNotSet(){
    Assertions.assertThrows(IllegalStateException.class, () -> {
      new MetadataBaseEntityBuilder().title("test-title").rights("test-rights").build();
    });
  }

  @Test
  public void throwsIfCreatorIsEmpty(){
    Assertions.assertThrows(IllegalStateException.class, () -> {
      new MetadataBaseEntityBuilder().title("test-title").rights("test-rights").creator("").build();
    });
  }

  @Test
  public void doesNotThrowIfDescriptionIsNotSet(){
    Assertions.assertDoesNotThrow(() -> {
      new MetadataBaseEntityBuilder().title("test-title").rights("test-rights").creator("test-creator").build();
    });
  }

  @Test
  public void doesNotThrowIfDescriptionIsEmpty(){
    Assertions.assertDoesNotThrow(() -> {
      new MetadataBaseEntityBuilder().title("test-title").rights("test-rights").creator("test-creator").description("").build();
    });
  }

  @Test
  public void mayBuildAMetadataBaseEntityWithExpectedValues(){
    MetadataBaseEntity metadataBaseEntity = new MetadataBaseEntityBuilder()
        .title("test-title")
        .rights("test-rights")
        .creator("test-creator")
        .description("test-description")
        .build();

    Assertions.assertEquals("test-title", metadataBaseEntity.getTitle());
    Assertions.assertEquals("test-rights", metadataBaseEntity.getRights());
    Assertions.assertEquals("test-creator", metadataBaseEntity.getCreator());
    Assertions.assertEquals("test-description", metadataBaseEntity.getDescription());
  }
}