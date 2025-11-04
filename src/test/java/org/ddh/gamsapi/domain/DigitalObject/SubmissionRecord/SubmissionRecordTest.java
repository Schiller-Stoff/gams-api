package org.ddh.gamsapi.domain.DigitalObject.SubmissionRecord;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.ddh.gamsapi.TestUtilities.TestBag;
import org.ddh.gamsapi.TestUtilities.TestDigitalObject;
import org.ddh.gamsapi.TestUtilities.TestSubmissionRecord;
import org.ddh.gamsapi.UnitTest;

public class SubmissionRecordTest extends UnitTest {

    @Test
    public void getBaggingDateReturnsExpectedValue(){
        var TEST_INGEST_RECORD = TestSubmissionRecord.generate(TestDigitalObject.generate());
        Assertions.assertThat(TEST_INGEST_RECORD
                .getBaggingDate()).isEqualTo(TestBag.TestBagInfo.BAGGING_DATE);
    }


}
