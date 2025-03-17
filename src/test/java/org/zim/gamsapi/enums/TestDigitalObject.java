package org.zim.gamsapi.enums;

import org.zim.gamsapi.DigitalObject.DigitalObject;
import org.zim.gamsapi.DigitalObject.DigitalObjectBuilder;

/**
 * Enum for test digital object.
 */
public enum TestDigitalObject {

    DIGITAL_OBJECT_ID("test.test"),
    DIGITAL_OBJECT_PROJECT_ABBR(TestProject.PROJECT_ABBR.getValue()),
    DIGITAL_OBJECT_NAME("test-digital-object"),
    DIGITAL_OBJECT_TYPE("test-digital-object-type"),
    DIGITAL_OBJECT_PUBLISHER("test-publisher"),
    DIGITAL_OBJECT_FUNDER("test-funder"),
    DIGITAL_OBJECT_TITLE(TestMetadataBaseEntity.TITLE),
    DIGITAL_OBJECT_DESCRIPTION(TestMetadataBaseEntity.DESCRIPTION),
    DIGITAL_OBJECT_CREATOR(TestMetadataBaseEntity.CREATOR),
    DIGITAL_OBJECT_RIGHTS(TestMetadataBaseEntity.RIGHTS);


    private final String value;

    TestDigitalObject(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    /**
     * Generates a test digital object using the values defined in the enum.
     * @param projectAbbr The abbreviation of the project.
     * @return The generated digital object.
     */
    public static DigitalObject generate(String projectAbbr){
        return generate(projectAbbr, DIGITAL_OBJECT_ID.getValue());
    }

    /**
     * Generates a test digital object using the values defined in the enum.
     * @return The generated digital object.
     */
    public static DigitalObject generate(){
        return generate(DIGITAL_OBJECT_PROJECT_ABBR.getValue(), DIGITAL_OBJECT_ID.getValue());
    }

    /**
     * Generates a test digital object using the values defined in the enum.
     * @param projectAbbr The abbreviation of the project.
     * @param id The id of the digital object to be created.
     * @return The generated digital object.
     */
    public static DigitalObject generate(String projectAbbr, String id){

        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("ID must not be null or empty");
        }
        if(!id.contains(projectAbbr + ".")){
            String msg = "DigitalObject ID does not contain project abbreviation with appended dot: " + id + " project: " + projectAbbr;
            throw new IllegalStateException(msg);
        }

        return new DigitalObjectBuilder()
            .id(id)
            .project(projectAbbr)
            .publisher(DIGITAL_OBJECT_PUBLISHER.getValue())
            .objectType(DIGITAL_OBJECT_TYPE.getValue())
            .funder(DIGITAL_OBJECT_FUNDER.getValue())
            .baseMetadata(TestMetadataBaseEntity.generate())
            .build();

    }

}
