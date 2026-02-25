package org.ddh.gamsapi.domain.Datastream;

import org.ddh.gamsapi.domain.Datastream.utils.ArchivalPolicy;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.ddh.gamsapi.UnitTest;

import java.util.Set;

public class DatastreamBuilderTest extends UnitTest {

  @Test
  public void doesNotThrowIfDigitalObjectIsNull(){
    Assertions.assertDoesNotThrow(() -> {
      new DatastreamBuilder()
          .dsid("dsid")
          .build();
    });
  }

  @Test
  public void doesNotthrowIfDigitalObjectIdIsEmpty(){
    Assertions.assertDoesNotThrow(() -> {
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

  @Test
  public void builderReturnsDatastreamBuilderInstance(){
    DatastreamBuilder datastreamBuilder = DatastreamBuilder.builder();
    Assertions.assertNotNull(datastreamBuilder);
  }

  @Test
  public void builtDatastreamContainsExpectedLang(){
    Set<String> stringSet = Set.of("lang1", "lang2", "lang3");
    Datastream datastream = new DatastreamBuilder()
        .digitalObject("digitalObjectId")
        .dsid("dsid")
        .lang(stringSet)
        .build();
    Assertions.assertNotNull(datastream);
    Assertions.assertEquals(stringSet, datastream.getLang());
  }

  @Test
  public void buildsWithDefaultArchivalPolicy() {
    Datastream ds = new DatastreamBuilder().dsid("test.txt").build();
    Assertions.assertEquals(ArchivalPolicy.DEFAULT, ds.getArchivalPolicy());
  }

  @Test
  public void buildsWithExplicitArchivalPolicy() {
    Datastream ds = new DatastreamBuilder()
        .dsid("test.txt")
        .archivalPolicy(ArchivalPolicy.FORCE_ARCHIVE)
        .build();
    Assertions.assertEquals(ArchivalPolicy.FORCE_ARCHIVE, ds.getArchivalPolicy());
  }

}
