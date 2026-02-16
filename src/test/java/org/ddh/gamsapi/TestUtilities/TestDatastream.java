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

    public static final String MD5_CHECKSUM = "827ccb0eea8a706c4c34a16891f84e7b";
    public static final String SHA512_CHECKSUM = "3627909a29c31381a071ec27f7c9ca97726182aed29a7ddd2e54353322cfb30abb9e3a6df2ac2c20fe23436311d678564d0c8d305930575f60e2d3d048184d79";

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
