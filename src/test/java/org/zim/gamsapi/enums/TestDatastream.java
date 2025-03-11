package org.zim.gamsapi.enums;

import org.zim.gamsapi.Datastream.Datastream;
import org.zim.gamsapi.Datastream.DatastreamBuilder;
import org.zim.gamsapi.DigitalObject.DigitalObject;

/**
 * Enum for test datastream.
 * Provides helpers for the generations of test datastreams.
 */
public enum TestDatastream {

    DSID("test.xml"),
    DATASTREAM_NAME("test-datastream");

    private final String value;

    TestDatastream(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    /**
     * Generates a test datastream using the values defined in the enum.
     * @return The generated datastream.
     */
    public static Datastream generate(){
        return generate(TestDigitalObject.generate());
    }

    /**
     * Generates a test datastream using the values defined in the enum.
     * @param digitalObject The digital object to which the datastream belongs.
     * @return The generated datastream.
     */
    public static Datastream generate(DigitalObject digitalObject){
        return generate(digitalObject, DSID.getValue());
    }

    /**
     * Generates a test datastream using the values defined in the enum.
     * @param digitalObject The digital object to which the datastream belongs.
     * @param dsid The dsid of the datastream.
     * @return The generated datastream.
     */
    public static Datastream generate(DigitalObject digitalObject, String dsid){
        return new DatastreamBuilder()
            .dsid(dsid)
            .digitalObject(digitalObject)
            .baseMetadata(TestMetadataBaseEntity.generate())
            .size( (long) TestDatastreamContent.CONTENT.getValue().length())
            .mimeType(TestDatastreamContent.CONTENT_TYPE.getValue())
            .fileName(TestDatastreamContent.ORIGINAL_FILENAME.getValue())
            .build();
    }
}
