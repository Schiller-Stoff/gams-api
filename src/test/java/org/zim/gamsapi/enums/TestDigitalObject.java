package org.zim.gamsapi.enums;

import org.zim.gamsapi.DigitalObject.DigitalObject;
import org.zim.gamsapi.DigitalObject.DigitalObjectBuilder;

/**
 * Enum for test digital object.
 */
public enum TestDigitalObject {

    DIGITAL_OBJECT_ID("test"),
    DIGITAL_OBJECT_NAME("test-digital-object");

    private final String value;

    TestDigitalObject(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static DigitalObject generate(String projectAbbr){
        return new DigitalObjectBuilder()
            .id(DIGITAL_OBJECT_ID.getValue())
            .project(projectAbbr)
            .publisher("test-publisher")
            .baseMetadata(TestMetadataBaseEntity.generate())
            .build();
    }

    public static DigitalObject generate(){
        return new DigitalObjectBuilder()
            .id(DIGITAL_OBJECT_ID.getValue())
            .project(TestProject.PROJECT_ABBR.getValue())
            .publisher("test-publisher")
            .baseMetadata(TestMetadataBaseEntity.generate())
            .build();
    }

    public static DigitalObject generate(String projectAbbr, String id){
        return new DigitalObjectBuilder()
            .id(id)
            .project(projectAbbr)
            .publisher("test-publisher")
            .baseMetadata(TestMetadataBaseEntity.generate())
            .build();
    }

}
