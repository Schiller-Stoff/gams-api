package org.zim.gamsapi.domain.DigitalObject.SubmissionRecord;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.zim.gamsapi.TestUtilities.TestSubmissionRecord;
import org.zim.gamsapi.TestUtilities.TestDigitalObject;
import org.zim.gamsapi.UnitTest;

public class SubmissionRecordBuilderTest extends UnitTest {

    @Test
    public void createsExpectedBagEntity(){
        SubmissionRecord TEST_BAG_ENTITY = SubmissionRecord.builder()
                .createdBy(TestSubmissionRecord.CREATED_BY)
                .schema(TestSubmissionRecord.SCHEMA)
                .source(TestSubmissionRecord.SOURCE)
                .baggingDate(TestSubmissionRecord.BAGGING_DATE)
                .contactMail(TestSubmissionRecord.CONTACT_EMAIL)
                .externalDescription(TestSubmissionRecord.EXTERNAL_DESCRIPTION)
                .payloadOxum(TestSubmissionRecord.PAYLOAD_OXUM)
                .bagVersion(TestSubmissionRecord.BAG_VERSION)
                .tagFileCharacterEncoding(TestSubmissionRecord.TAG_FILE_CHARACTER_ENCODING)
                .digitalObject(TestDigitalObject.generate())
                .build();

        Assertions.assertThat(TEST_BAG_ENTITY).isNotNull();
        Assertions.assertThat(TEST_BAG_ENTITY).hasNoNullFieldsOrPropertiesExcept("id");

        Assertions.assertThat(TEST_BAG_ENTITY.getBagCreatedBy()).isEqualTo(TestSubmissionRecord.CREATED_BY);
        Assertions.assertThat(TEST_BAG_ENTITY.getBagSchema()).isEqualTo(TestSubmissionRecord.SCHEMA);
        Assertions.assertThat(TEST_BAG_ENTITY.getBagSource()).isEqualTo(TestSubmissionRecord.SOURCE);
        Assertions.assertThat(TEST_BAG_ENTITY.getBaggingDate()).isEqualTo(TestSubmissionRecord.BAGGING_DATE);
        Assertions.assertThat(TEST_BAG_ENTITY.getBagContactMail()).isEqualTo(TestSubmissionRecord.CONTACT_EMAIL);
        Assertions.assertThat(TEST_BAG_ENTITY.getBagExternalDescription()).isEqualTo(TestSubmissionRecord.EXTERNAL_DESCRIPTION);
        Assertions.assertThat(TEST_BAG_ENTITY.getBagPayloadOxum()).isEqualTo(TestSubmissionRecord.PAYLOAD_OXUM);
        Assertions.assertThat(TEST_BAG_ENTITY.getBagVersion()).isEqualTo(TestSubmissionRecord.BAG_VERSION);
        Assertions.assertThat(TEST_BAG_ENTITY.getBagTagFileCharacterEncoding()).isEqualTo(TestSubmissionRecord.TAG_FILE_CHARACTER_ENCODING);
        Assertions.assertThat(TEST_BAG_ENTITY.getDigitalObject().getId()).isEqualTo(TestDigitalObject.generate().getId());
        Assertions.assertThat(TEST_BAG_ENTITY.getDigitalObject().getId()).isEqualTo(TestDigitalObject.DIGITAL_OBJECT_ID.getValue());

    }

    @Test
    public void throwsIllegalStateExceptionIfDigitalObjectIsNotSet(){
        Assertions.assertThatThrownBy(()->{
            SubmissionRecord.builder()
                    .createdBy(TestSubmissionRecord.CREATED_BY)
                    .schema(TestSubmissionRecord.SCHEMA)
                    .source(TestSubmissionRecord.SOURCE)
                    .baggingDate(TestSubmissionRecord.BAGGING_DATE)
                    .contactMail(TestSubmissionRecord.CONTACT_EMAIL)
                    .build();
        }).isInstanceOf(IllegalStateException.class);

    }

}
