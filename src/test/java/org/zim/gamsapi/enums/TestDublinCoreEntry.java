package org.zim.gamsapi.enums;

import org.zim.gamsapi.DigitalObject.DublinCoreEntry.DublinCoreEntry;

/**
 * Enum for DublinCoreEntry.
 */
public enum TestDublinCoreEntry {

  NAME("title"),
  VALUE("test-dc-title"),
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

  public static DublinCoreEntry generate(String objectId){
    return DublinCoreEntry.builder()
        .name(NAME.getValue())
        .value(VALUE.getValue())
        .digitalObject(
            TestDigitalObject.generate(PROJECT_ABBR.getValue(), objectId)
        ).build();
  }

//  public static DublinCoreEntry generate(){
//    return generate(OBJECT_ID.getValue());
//  }
}
