package org.zim.gamsapi.domain.DigitalObject.SubmissionRecord;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.zim.gamsapi.TestUtilities.TestIngestRecord;
import org.zim.gamsapi.TestUtilities.TestDigitalObject;
import org.zim.gamsapi.UnitTest;

public class SubmissionRecordBuilderTest extends UnitTest {

    @Test
    public void createsExpectedBagEntity(){
        SubmissionRecord TEST_BAG_ENTITY = SubmissionRecord.builder()
                .createdBy(TestIngestRecord.CREATED_BY)
                .schema(TestIngestRecord.SCHEMA)
                .source(TestIngestRecord.SOURCE)
                .baggingTimeStamp(TestIngestRecord.BAGGING_TIMESTAMP)
                .contactMail(TestIngestRecord.CONTACT_EMAIL)
                .externalDescription(TestIngestRecord.EXTERNAL_DESCRIPTION)
                .payloadOxum(TestIngestRecord.PAYLOAD_OXUM)
                .bagVersion(TestIngestRecord.BAG_VERSION)
                .tagFileCharacterEncoding(TestIngestRecord.TAG_FILE_CHARACTER_ENCODING)
                .digitalObject(TestDigitalObject.generate())
                .build();

        Assertions.assertThat(TEST_BAG_ENTITY).isNotNull();
        Assertions.assertThat(TEST_BAG_ENTITY).hasNoNullFieldsOrPropertiesExcept("id");

        Assertions.assertThat(TEST_BAG_ENTITY.getBagCreatedBy()).isEqualTo(TestIngestRecord.CREATED_BY);
        Assertions.assertThat(TEST_BAG_ENTITY.getBagSchema()).isEqualTo(TestIngestRecord.SCHEMA);
        Assertions.assertThat(TEST_BAG_ENTITY.getBagSource()).isEqualTo(TestIngestRecord.SOURCE);
        Assertions.assertThat(TEST_BAG_ENTITY.getBaggingTimeStamp()).isEqualTo(TestIngestRecord.BAGGING_TIMESTAMP);
        Assertions.assertThat(TEST_BAG_ENTITY.getBagContactMail()).isEqualTo(TestIngestRecord.CONTACT_EMAIL);
        Assertions.assertThat(TEST_BAG_ENTITY.getBagExternalDescription()).isEqualTo(TestIngestRecord.EXTERNAL_DESCRIPTION);
        Assertions.assertThat(TEST_BAG_ENTITY.getBagPayloadOxum()).isEqualTo(TestIngestRecord.PAYLOAD_OXUM);
        Assertions.assertThat(TEST_BAG_ENTITY.getBagVersion()).isEqualTo(TestIngestRecord.BAG_VERSION);
        Assertions.assertThat(TEST_BAG_ENTITY.getBagTagFileCharacterEncoding()).isEqualTo(TestIngestRecord.TAG_FILE_CHARACTER_ENCODING);
        Assertions.assertThat(TEST_BAG_ENTITY.getDigitalObject().getId()).isEqualTo(TestDigitalObject.generate().getId());
        Assertions.assertThat(TEST_BAG_ENTITY.getDigitalObject().getId()).isEqualTo(TestDigitalObject.DIGITAL_OBJECT_ID.getValue());

    }

    @Test
    public void throwsIllegalStateExceptionIfDigitalObjectIsNotSet(){
        Assertions.assertThatThrownBy(()->{
            SubmissionRecord.builder()
                    .createdBy(TestIngestRecord.CREATED_BY)
                    .schema(TestIngestRecord.SCHEMA)
                    .source(TestIngestRecord.SOURCE)
                    .baggingTimeStamp(TestIngestRecord.BAGGING_TIMESTAMP)
                    .contactMail(TestIngestRecord.CONTACT_EMAIL)
                    .build();
        }).isInstanceOf(IllegalStateException.class);

    }

}
