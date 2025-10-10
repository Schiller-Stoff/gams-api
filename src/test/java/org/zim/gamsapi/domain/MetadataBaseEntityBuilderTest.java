package org.zim.gamsapi.domain;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.zim.gamsapi.MetadataBaseEntity;
import org.zim.gamsapi.MetadataBaseEntityBuilder;
import org.zim.gamsapi.UnitTest;

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
  public void throwsIfMd5ChecksumIsNot32CharactersLong(){
    Assertions.assertThrows(IllegalArgumentException.class, () -> {
      new MetadataBaseEntityBuilder().title("test-title").rights("test-rights").creator("test-creator").md5Checksum("too-short").build();
    });
  }

  @Test
  public void throwsIfSha512ChecksumIsNot128CharactersLong(){
    Assertions.assertThrows(IllegalArgumentException.class, () -> {
      new MetadataBaseEntityBuilder().title("test-title").rights("test-rights").creator("test-creator").sha512Checksum("too-short").build();
    });
  }

  @Test
  public void mayBuildAMetadataBaseEntityWithExpectedValues(){

    final String MD5_CHECKSUM = "d41d8cd98f00b204e9800998ecf8427e";
    final String SHA512_CHECKSUM = "cf83e1357eefb8bdf1542850d66d8007d620e4050b5715dc83f4a921d14b2b0b822cd15d6c15b0f00a083e9b0c4b1d6f0b6bd6f0a2c4e6f7a5a6e0e5f8f5be5f";

    MetadataBaseEntity metadataBaseEntity = new MetadataBaseEntityBuilder()
        .title("test-title")
        .rights("test-rights")
        .creator("test-creator")
        .description("test-description")
        .md5Checksum(MD5_CHECKSUM)
        .sha512Checksum(SHA512_CHECKSUM)
        .build();

    Assertions.assertEquals("test-title", metadataBaseEntity.getTitle());
    Assertions.assertEquals("test-rights", metadataBaseEntity.getRights());
    Assertions.assertEquals("test-creator", metadataBaseEntity.getCreator());
    Assertions.assertEquals("test-description", metadataBaseEntity.getDescription());
    Assertions.assertEquals(MD5_CHECKSUM, metadataBaseEntity.getMd5Checksum());
    Assertions.assertEquals(SHA512_CHECKSUM, metadataBaseEntity.getSha512Checksum());
  }
}