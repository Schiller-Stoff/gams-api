package org.zim.gamsapi.TestUtilities;

import lombok.extern.slf4j.Slf4j;
import org.zim.gamsapi.domain.DigitalObjectCollection.DigitalObjectCollection;
import org.zim.gamsapi.domain.DigitalObject.DigitalObject;

import java.util.Set;

@Slf4j
public enum TestGAMSCollection {

  ID("test-collection-id"),
  TITLE("test-collection-title"),
  DESCRIPTION("test-collection-description"),
  PROJECT_ABBR(TestProject.PROJECT_ABBR.getValue());

  public static final Set<DigitalObject> DIGITAL_OBJECTS = Set.of(TestDigitalObject.generate());

  private final String value;

  TestGAMSCollection(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }

  public static DigitalObjectCollection generate(String projectAbbr, String digitalObjectId, String collectionId){

    if(!digitalObjectId.contains(projectAbbr)){
      String msg = String.format("Digital object ID %s must contain the project abbreviation %s", digitalObjectId, projectAbbr);
      log.error(msg);
      throw new IllegalArgumentException(msg);
    }

    return DigitalObjectCollection.builder()
        .id(collectionId)
        .title(TITLE.getValue())
        .description(DESCRIPTION.getValue())
        .project(TestProject.generate(projectAbbr))
        .digitalObjects(
            Set.of(TestDigitalObject.generate(projectAbbr, digitalObjectId))
        )
        .build();
  }

  public static DigitalObjectCollection generate(){
    return DigitalObjectCollection.builder()
        .id(ID.getValue())
        .title(TITLE.getValue())
        .description(DESCRIPTION.getValue())
        .project(TestProject.generate())
        .digitalObjects(DIGITAL_OBJECTS)
        .build();
  }

}
