package org.ddh.gamsapi.TestUtilities;

import org.ddh.gamsapi.domain.DigitalObject.DigitalObject;
import org.ddh.gamsapi.domain.DigitalObject.DigitalObjectBuilder;

import java.util.HashSet;
import java.util.Set;

/**
 * Enum for test digital object.
 */
public enum TestDigitalObject {

    DIGITAL_OBJECT_ID("test.test"),
    DIGITAL_OBJECT_PROJECT_ABBR(TestProject.PROJECT_ABBR.getValue()),
    DIGITAL_OBJECT_NAME("test-digital-object"),
    DIGITAL_OBJECT_TYPE("TEI"),
    DIGITAL_OBJECT_PUBLISHER("test-publisher"),
    DIGITAL_OBJECT_FUNDER("test-funder"),
    DIGITAL_OBJECT_TITLE(TestMetadataBaseEntity.TITLE),
    DIGITAL_OBJECT_DESCRIPTION(TestMetadataBaseEntity.DESCRIPTION),
    DIGITAL_OBJECT_CREATOR(TestMetadataBaseEntity.CREATOR),
    DIGITAL_OBJECT_RIGHTS(TestMetadataBaseEntity.RIGHTS),
    DIGITAL_OBJECT_MAIN_RESOURCE(TestDatastream.DSID.getValue()),
    DIGITAL_OBJECT_MD5_CHECKSUM(TestMetadataBaseEntity.MD5_CHECKSUM),
    DIGITAL_OBJECT_SHA512_CHECKSUM(TestMetadataBaseEntity.SHA512_CHECKSUM);

    private final String value;
    private static final Set<String> DIGITAL_OBJECT_TAGS = Set.of("object-test-tag1", "object-test-tag2", "object-test-tag3");

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
     * Returns a copied set of tags for the test digital object.
     * @return The set of tags.
     */
    public static Set<String> getTags() {
        return new HashSet<>(DIGITAL_OBJECT_TAGS);
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
            .mainResource(DIGITAL_OBJECT_MAIN_RESOURCE.getValue())
            .tags(TestDigitalObject.getTags())
            .baseMetadata(TestMetadataBaseEntity.generate())
            .build();

    }

}
