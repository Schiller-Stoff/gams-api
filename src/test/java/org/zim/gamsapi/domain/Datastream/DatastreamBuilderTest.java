package org.zim.gamsapi.domain.Datastream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.zim.gamsapi.UnitTest;
import org.zim.gamsapi.domain.Datastream.Datastream;
import org.zim.gamsapi.domain.Datastream.DatastreamBuilder;

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
  public void ableToDefineContentRestrictions(){

    Set<String> stringSet = Set.of("restriction1", "restriction2");
    Datastream datastream = new DatastreamBuilder()
        .digitalObject("digitalObjectId")
        .dsid("dsid")
        .contentRestrictions(stringSet)
        .build();

    Assertions.assertNotNull(datastream);
    Assertions.assertEquals("digitalObjectId", datastream.getDigitalObject().getId());
    Assertions.assertEquals("dsid", datastream.getDsid());
    Assertions.assertEquals(stringSet, datastream.getContentRestrictions());

  }

  @Test
  public void builtDatastreamContainsExpectedTags(){
    Set<String> stringSet = Set.of("tag1", "tag2", "tag3");
    Datastream datastream = new DatastreamBuilder()
        .digitalObject("digitalObjectId")
        .dsid("dsid")
        .tags(stringSet)
        .build();
    Assertions.assertNotNull(datastream);
    Assertions.assertEquals(stringSet, datastream.getTags());
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

}
