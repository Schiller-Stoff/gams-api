package org.zim.gamsapi.Ingest;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.zim.gamsapi.TestUtilities.TestBagEntity;
import org.zim.gamsapi.TestUtilities.TestDigitalObject;
import org.zim.gamsapi.UnitTest;

public class BagEntityBuilderTest extends UnitTest {

    @Test
    public void createsExpectedBagEntity(){
        BagEntity TEST_BAG_ENTITY = BagEntity.builder()
                .createdBy(TestBagEntity.CREATED_BY)
                .schema(TestBagEntity.SCHEMA)
                .source(TestBagEntity.SOURCE)
                .baggingTimeStamp(TestBagEntity.BAGGING_TIMESTAMP)
                .contactMail(TestBagEntity.CONTACT_EMAIL)
                .externalDescription(TestBagEntity.EXTERNAL_DESCRIPTION)
                .payloadOxum(TestBagEntity.PAYLOAD_OXUM)
                .bagVersion(TestBagEntity.BAG_VERSION)
                .tagFileCharacterEncoding(TestBagEntity.TAG_FILE_CHARACTER_ENCODING)
                .digitalObject(TestDigitalObject.generate())
                .build();

        Assertions.assertThat(TEST_BAG_ENTITY).isNotNull();
        Assertions.assertThat(TEST_BAG_ENTITY).hasNoNullFieldsOrPropertiesExcept("id");

        Assertions.assertThat(TEST_BAG_ENTITY.getCreatedBy()).isEqualTo(TestBagEntity.CREATED_BY);
        Assertions.assertThat(TEST_BAG_ENTITY.getSchema()).isEqualTo(TestBagEntity.SCHEMA);
        Assertions.assertThat(TEST_BAG_ENTITY.getSource()).isEqualTo(TestBagEntity.SOURCE);
        Assertions.assertThat(TEST_BAG_ENTITY.getBaggingTimeStamp()).isEqualTo(TestBagEntity.BAGGING_TIMESTAMP);
        Assertions.assertThat(TEST_BAG_ENTITY.getContactMail()).isEqualTo(TestBagEntity.CONTACT_EMAIL);
        Assertions.assertThat(TEST_BAG_ENTITY.getExternalDescription()).isEqualTo(TestBagEntity.EXTERNAL_DESCRIPTION);
        Assertions.assertThat(TEST_BAG_ENTITY.getPayloadOxum()).isEqualTo(TestBagEntity.PAYLOAD_OXUM);
        Assertions.assertThat(TEST_BAG_ENTITY.getBagVersion()).isEqualTo(TestBagEntity.BAG_VERSION);
        Assertions.assertThat(TEST_BAG_ENTITY.getTagFileCharacterEncoding()).isEqualTo(TestBagEntity.TAG_FILE_CHARACTER_ENCODING);
        Assertions.assertThat(TEST_BAG_ENTITY.getDigitalObject().getId()).isEqualTo(TestDigitalObject.generate().getId());
        Assertions.assertThat(TEST_BAG_ENTITY.getDigitalObject().getId()).isEqualTo(TestDigitalObject.DIGITAL_OBJECT_ID.getValue());

    }

    @Test
    public void throwsIllegalStateExceptionIfDigitalObjectIsNotSet(){
        Assertions.assertThatThrownBy(()->{
            BagEntity.builder()
                    .createdBy(TestBagEntity.CREATED_BY)
                    .schema(TestBagEntity.SCHEMA)
                    .source(TestBagEntity.SOURCE)
                    .baggingTimeStamp(TestBagEntity.BAGGING_TIMESTAMP)
                    .contactMail(TestBagEntity.CONTACT_EMAIL)
                    .build();
        }).isInstanceOf(IllegalStateException.class);

    }

}
