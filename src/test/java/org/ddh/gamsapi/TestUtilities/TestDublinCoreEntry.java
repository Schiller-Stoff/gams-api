package org.ddh.gamsapi.TestUtilities;

import org.ddh.gamsapi.domain.DigitalObject.DigitalObject;
import org.ddh.gamsapi.domain.DigitalObject.DublinCoreEntry.DublinCoreEntry;

/**
 * Enum for DublinCoreEntry.
 */
public enum TestDublinCoreEntry {

  NAME("title"),
  VALUE("test-dc-title"),
  LANGUAGE("en"),
  OBJECT_ID(TestDigitalObject.DIGITAL_OBJECT_ID.getValue()),
  PROJECT_ABBR(TestProject.PROJECT_ABBR.getValue()),
  ;

  private final String value;

  TestDublinCoreEntry(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }

  public static DublinCoreEntry generate(DigitalObject digitalObject){
    return DublinCoreEntry.builder()
        .name(NAME.getValue())
        .value(VALUE.getValue())
        .language(LANGUAGE.getValue())
        .digitalObject(digitalObject).build();
  }

  public static DublinCoreEntry generate(String objectId){
    return generate(PROJECT_ABBR.getValue(), objectId);
  }

  public static DublinCoreEntry generate(String projectAbbr, String objectId){
    return DublinCoreEntry.builder()
        .name(NAME.getValue())
        .value(VALUE.getValue())
        .language(LANGUAGE.getValue())
        .digitalObject(
            TestDigitalObject.generate(projectAbbr, objectId)
        ).build();
  }

//  public static DublinCoreEntry generate(){
//    return generate(OBJECT_ID.getValue());
//  }
}
