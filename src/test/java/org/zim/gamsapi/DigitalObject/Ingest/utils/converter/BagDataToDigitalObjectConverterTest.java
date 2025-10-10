package org.zim.gamsapi.DigitalObject.Ingest.utils.converter;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.zim.gamsapi.DigitalObject.DigitalObject;
import org.zim.gamsapi.DigitalObject.Ingest.utils.Bagit.BagData;
import org.zim.gamsapi.DigitalObject.Ingest.utils.converter.BagDataToDigitalObjectConverter;
import org.zim.gamsapi.TestUtilities.TestBag;
import org.zim.gamsapi.UnitTest;
import java.util.Set;

public class BagDataToDigitalObjectConverterTest extends UnitTest {

    private final BagData TEST_BAG_DATA = BagData.builder()
            .id(TestBag.TestBagSipJson.REC_ID)
            .project(TestBag.TestBagSipJson.PROJECT)
            .objectType(TestBag.TestBagSipJson.OBJECT_TYPE)
            .title(TestBag.TestBagSipJson.TITLE)
            .types(Set.of("testTypes"))
            .creator(TestBag.TestBagSipJson.CREATOR)
            .description(TestBag.TestBagSipJson.DESCRIPTION)
            .rights(TestBag.TestBagSipJson.RIGHTS)
            .publisher(TestBag.TestBagSipJson.PUBLISHER)
            .funder(TestBag.TestBagSipJson.FUNDER)
            .mainResource(TestBag.TestBagSipJson.MAIN_RESOURCE)
            .md5Checksum("540193d9633d8449ee1bff28030fe045")
            .sha512Checksum("61eb68db4754a8349405f9355e86a72f32b00e17b747662c06c1c3027997d26d3cb1907e5f3ee3ec8299d67d97dc7c7ff4844dc70db8c5226666faf121540009")
            .source(TestBag.TestBagSipJson.SOURCE)
            .schema(TestBag.TestBagSipJson.SCHEMA)
            .createdBy(TestBag.TestBagSipJson.CREATED_BY)
            .build();

    @Test
    public void convertsBagDataToExpectedDigitalObject(){

        BagDataToDigitalObjectConverter bagDataToDigitalObjectConverter = new BagDataToDigitalObjectConverter();
        DigitalObject convertedDigitalObject = bagDataToDigitalObjectConverter.convert(TEST_BAG_DATA);

        Assertions.assertThat(convertedDigitalObject).isNotNull();
        Assertions.assertThat(convertedDigitalObject.getId()).isEqualTo(TEST_BAG_DATA.getId());
        Assertions.assertThat(convertedDigitalObject.getProject().getProjectAbbr()).isEqualTo(TEST_BAG_DATA.getProject());
        Assertions.assertThat(convertedDigitalObject.getObjectType()).isEqualTo(TEST_BAG_DATA.getObjectType());
        Assertions.assertThat(convertedDigitalObject.getBaseMetadata().getTitle()).isEqualTo(TEST_BAG_DATA.getTitle());
        Assertions.assertThat(convertedDigitalObject.getBaseMetadata().getCreator()).isEqualTo(TEST_BAG_DATA.getCreator());
        Assertions.assertThat(convertedDigitalObject.getBaseMetadata().getDescription()).isEqualTo(TEST_BAG_DATA.getDescription());
        Assertions.assertThat(convertedDigitalObject.getBaseMetadata().getRights()).isEqualTo(TEST_BAG_DATA.getRights());
        Assertions.assertThat(convertedDigitalObject.getPublisher()).isEqualTo(TEST_BAG_DATA.getPublisher());
        Assertions.assertThat(convertedDigitalObject.getMainResource()).isEqualTo(TEST_BAG_DATA.getMainResource());

        Assertions.assertThat(convertedDigitalObject.getBaseMetadata().getMd5Checksum()).isEqualTo(TEST_BAG_DATA.getMd5Checksum());
        Assertions.assertThat(convertedDigitalObject.getBaseMetadata().getSha512Checksum()).isEqualTo(TEST_BAG_DATA.getSha512Checksum());

        Assertions.assertThat(convertedDigitalObject.getFunder()).isEqualTo(TEST_BAG_DATA.getFunder());
    }

    @Test
    public void throwsIllegalStateIfObjectIdWouldBeNull(){

        BagData bagData = BagData.builder()
                .build();

        BagDataToDigitalObjectConverter bagitSipJsonDigitalObjectConverter = new BagDataToDigitalObjectConverter();
        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, () -> bagitSipJsonDigitalObjectConverter.convert(bagData));
    }

    @Test
    public void convertedObjectIsNotNull(){
        BagDataToDigitalObjectConverter bagDataToDigitalObjectConverter = new BagDataToDigitalObjectConverter();
        DigitalObject convertedDigitalObject = bagDataToDigitalObjectConverter.convert(TEST_BAG_DATA);
        Assertions.assertThat(convertedDigitalObject).isNotNull();
    }

    @Test
    public void expectedDigitalObjectHasNoNullFields(){
        BagDataToDigitalObjectConverter bagDataToDigitalObjectConverter = new BagDataToDigitalObjectConverter();
        DigitalObject convertedDigitalObject = bagDataToDigitalObjectConverter.convert(TEST_BAG_DATA);
        Assertions.assertThat(convertedDigitalObject).hasNoNullFieldsOrPropertiesExcept(
                "created", "published", "modified", "createdBy", "modifiedBy"
        );
    }

}
