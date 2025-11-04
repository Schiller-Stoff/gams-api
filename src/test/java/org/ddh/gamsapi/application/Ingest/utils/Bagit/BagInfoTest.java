package org.ddh.gamsapi.application.Ingest.utils.Bagit;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.ddh.gamsapi.TestUtilities.TestBag;
import org.ddh.gamsapi.domain.DigitalObject.SubmissionRecord.SubmissionRecord;
import org.ddh.gamsapi.TestUtilities.TestDigitalObject;
import org.ddh.gamsapi.TestUtilities.TestSubmissionRecord;
import org.ddh.gamsapi.UnitTest;

public class BagInfoTest extends UnitTest {

    @Nested
    public class From {

        SubmissionRecord TEST_INGEST_RECORD = TestSubmissionRecord.generate(TestDigitalObject.generate());

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
            Assertions.assertThat(bagInfo.getPayloadOxum()).isEqualTo(TEST_INGEST_RECORD.getBagPayloadOxum());
            Assertions.assertThat(bagInfo.getContactMail()).isEqualTo(TEST_INGEST_RECORD.getBagContactMail());
            Assertions.assertThat(bagInfo.getExternalDescription()).isEqualTo(TEST_INGEST_RECORD.getBagExternalDescription());
        }


    }

    @Nested
    public class ToBagInfoContent {

      @Test
      public void toBagInfoContentReturnsNonNullOrEmptyString(){
        var bagInfo = TestBag.TestBagInfo.generate();
        var content = bagInfo.toBagInfoContent();
        Assertions.assertThat(content)
            .isNotNull()
            .isNotEmpty();

      }

      @Test
      public void containsExpectedValues(){
        var bagInfo = TestBag.TestBagInfo.generate();
        var content = bagInfo.toBagInfoContent();

        Assertions.assertThat(content).contains(
            TestBag.TestBagInfo.BAGGING_DATE,
            TestBag.TestBagInfo.CONTACT_EMAIL,
            TestBag.TestBagInfo.EXTERNAL_DESCRIPTION,
            TestBag.TestBagInfo.PAYLOAD_OXUM.toString()
        );

      }


    }

}
