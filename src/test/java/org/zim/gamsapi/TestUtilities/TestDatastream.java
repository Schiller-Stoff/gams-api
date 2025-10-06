package org.zim.gamsapi.TestUtilities;

import org.zim.gamsapi.Datastream.Datastream;
import org.zim.gamsapi.Datastream.DatastreamBuilder;
import org.zim.gamsapi.DigitalObject.DigitalObject;
import org.zim.gamsapi.MetadataBaseEntity;

import java.util.Set;

/**
 * Enum for test datastream.
 * Provides helpers for the generations of test datastreams.
 */
public enum TestDatastream {

    DSID("test.txt"),
    DATASTREAM_NAME("test-datastream"),
    FILE_NAME(TestDatastreamContent.ORIGINAL_FILENAME.getValue()),
    MIME_TYPE(TestDatastreamContent.CONTENT_TYPE.getValue())

    ;

    public static final Set<String> DATASTREAM_TAGS = Set.of("test-tag1", "test-tag2", "test-tag3");
    public static final Set<String> DATASTREAM_LANG = Set.of("test-lang1", "test-lang2", "test-lang3");
    public static final MetadataBaseEntity METADATA_BASE_ENTITY = TestMetadataBaseEntity.generate();

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
            .tags(DATASTREAM_TAGS)
            .baseMetadata(METADATA_BASE_ENTITY)
            .size( (long) TestDatastreamContent.CONTENT.getValue().length())
            .mimeType(MIME_TYPE.getValue())
            .bagPath(FILE_NAME.getValue())
            .lang(DATASTREAM_LANG)
            .build();
    }
}
