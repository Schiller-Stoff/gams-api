package org.zim.gamsapi.application.Ingest.utils.converter;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.zim.gamsapi.application.Ingest.utils.Bagit.BagFile;
import org.zim.gamsapi.application.Ingest.utils.converter.BagFileToDatastreamConverter;
import org.zim.gamsapi.TestUtilities.TestDatastream;
import org.zim.gamsapi.UnitTest;

public class BagFileToDatastreamConverterTest extends UnitTest {

    final BagFile TEST_BAG_FILE = BagFile.builder()
            .bagpath("data/file.txt")
            .creator("unitTest")
            .md5Checksum("540193d9633d8449ee1bff28030fe045")
            .sha512Checksum("61eb68db4754a8349405f9355e86a72f32b00e17b747662c06c1c3027997d26d3cb1907e5f3ee3ec8299d67d97dc7c7ff4844dc70db8c5226666faf121540009")
            .description("unit test file")
            .size(12345L)
            .title("test title")
            .dsid(TestDatastream.DSID.getValue())
            .tags(TestDatastream.DATASTREAM_TAGS)
            .lang(TestDatastream.DATASTREAM_LANG)
            .rights(TestDatastream.METADATA_BASE_ENTITY.getRights())
            .mimetype(TestDatastream.MIME_TYPE.getValue())
            .build();

    @Test
    public void convertedDatastreamIsNotNull(){
        BagFileToDatastreamConverter converter = new BagFileToDatastreamConverter();
        var convertedDatastream = converter.convert(TEST_BAG_FILE);
        Assertions.assertThat(convertedDatastream).isNotNull();
    }

    @Test
    public void convertedDatastreamHasNoNullFields(){
        BagFileToDatastreamConverter converter = new BagFileToDatastreamConverter();
        var convertedDatastream = converter.convert(TEST_BAG_FILE);
        Assertions.assertThat(convertedDatastream)
                .hasNoNullFieldsOrPropertiesExcept(
                        "fileName", "type", "created", "modified", "createdBy", "modifiedBy", "digitalObject"
                );
    }

    @Test
    public void convertedDatastreamHasExpectedValues(){

        BagFileToDatastreamConverter converter = new BagFileToDatastreamConverter();
        var convertedDatastream = converter.convert(TEST_BAG_FILE);

        Assertions.assertThat(convertedDatastream).isNotNull();
        Assertions.assertThat(convertedDatastream.getDsid()).isEqualTo(TEST_BAG_FILE.getDsid());
        Assertions.assertThat(convertedDatastream.getBaseMetadata().getTitle()).isEqualTo(TEST_BAG_FILE.getTitle());
        Assertions.assertThat(convertedDatastream.getBaseMetadata().getDescription()).isEqualTo(TEST_BAG_FILE.getDescription());
        Assertions.assertThat(convertedDatastream.getBaseMetadata().getCreator()).isEqualTo(TEST_BAG_FILE.getCreator());
        Assertions.assertThat(convertedDatastream.getBaseMetadata().getMd5Checksum()).isEqualTo(TEST_BAG_FILE.getMd5Checksum());
        Assertions.assertThat(convertedDatastream.getBaseMetadata().getSha512Checksum()).isEqualTo(TEST_BAG_FILE.getSha512Checksum());
        Assertions.assertThat(convertedDatastream.getSize()).isEqualTo(TEST_BAG_FILE.getSize());
        Assertions.assertThat(convertedDatastream.getTags()).isEqualTo(TEST_BAG_FILE.getTags());
        Assertions.assertThat(convertedDatastream.getLang()).isEqualTo(TEST_BAG_FILE.getLang());

        Assertions.assertThat(convertedDatastream.getBagPath()).isEqualTo(TEST_BAG_FILE.getBagpath());
        Assertions.assertThat(convertedDatastream.getMimeType()).isEqualTo(TEST_BAG_FILE.getMimetype());

    }

}
