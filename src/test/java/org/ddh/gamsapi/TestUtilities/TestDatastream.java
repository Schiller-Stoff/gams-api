package org.ddh.gamsapi.TestUtilities;

import org.ddh.gamsapi.domain.Datastream.Datastream;
import org.ddh.gamsapi.domain.Datastream.DatastreamBuilder;
import org.ddh.gamsapi.domain.DigitalObject.DigitalObject;
import org.ddh.gamsapi.domain.MetadataBaseEntity;

import java.util.Set;

/**
 * Enum for test datastream.
 * Provides helpers for the generations of test datastreams.
 */
public enum TestDatastream {

    DSID("test.txt"),
    DATASTREAM_NAME("test-datastream"),
    FILE_NAME(TestDatastreamContent.ORIGINAL_FILENAME.getValue()),
    MIME_TYPE(TestDatastreamContent.CONTENT_TYPE.getValue()),
    BAG_PATH(TestDatastreamContent.BAG_PATH.getValue());

    public static final Set<String> DATASTREAM_TAGS = Set.of("test-tag1", "test-tag2", "test-tag3");
    public static final Set<String> DATASTREAM_LANG = Set.of("test-lang1", "test-lang2", "test-lang3");

    public static final String MD5_CHECKSUM = "240193d9633d8449ee1bff28030fe045";
    public static final String SHA512_CHECKSUM = "31eb68db4754a8349405f9355e86a72f32b00e17b747662c06c1c3027997d26d3cb1907e5f3ee3ec8299d67d97dc7c7ff4844dc70db8c5226666faf121540009";

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
    public static Datastream generate(DigitalObject digitalObject, String dsid) {
      return new DatastreamBuilder()
          .dsid(dsid)
          .digitalObject(digitalObject)
          .tags(DATASTREAM_TAGS)
          .baseMetadata(TestMetadataBaseEntity.generate())
          .md5Checksum(MD5_CHECKSUM)
          .sha512Checksum(SHA512_CHECKSUM)
          .size((long) TestDatastreamContent.CONTENT.getValue().length())
          .mimeType(MIME_TYPE.getValue())
          .bagPath(FILE_NAME.getValue())
          .lang(DATASTREAM_LANG)
          .build();
    }

}
