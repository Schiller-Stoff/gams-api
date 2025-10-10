package org.zim.gamsapi.application.Ingest.utils.Bagit;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.zim.gamsapi.application.Ingest.SubmissionRecord;
import org.zim.gamsapi.TestUtilities.TestDigitalObject;
import org.zim.gamsapi.TestUtilities.TestIngestRecord;
import org.zim.gamsapi.UnitTest;

public class BagInfoTest extends UnitTest {

    @Nested
    public class From {

        SubmissionRecord TEST_INGEST_RECORD = TestIngestRecord.generate(TestDigitalObject.generate());

        @Test
        public void fromIngestRecordReturnsNonNullObject(){
            var bagInfo = BagInfo.from(TEST_INGEST_RECORD);
            Assertions.assertThat(bagInfo).isNotNull();

        }

        @Test
        public void fromIngestRecordReturnsBagInfoWithNoNullFields(){
            var bagInfo = BagInfo.from(TEST_INGEST_RECORD);
            Assertions.assertThat(bagInfo).hasNoNullFieldsOrProperties();
        }

        @Test
        public void constructsExpectedBagInfo(){
            var bagInfo = BagInfo.from(TEST_INGEST_RECORD);
            Assertions.assertThat(bagInfo.getDate()).isEqualTo(TEST_INGEST_RECORD.getBaggingDate());
            Assertions.assertThat(bagInfo.getTime()).isEqualTo(TEST_INGEST_RECORD.getBaggingTime());
            Assertions.assertThat(bagInfo.getBaggingTimeStamp()).isEqualTo(TEST_INGEST_RECORD.getBaggingTimeStamp());
            Assertions.assertThat(bagInfo.getPayloadOxum()).isEqualTo(TEST_INGEST_RECORD.getBagPayloadOxum());
            Assertions.assertThat(bagInfo.getContactMail()).isEqualTo(TEST_INGEST_RECORD.getBagContactMail());
            Assertions.assertThat(bagInfo.getExternalDescription()).isEqualTo(TEST_INGEST_RECORD.getBagExternalDescription());
        }


    }

}
