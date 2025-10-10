package org.zim.gamsapi.application.Ingest;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.zim.gamsapi.TestUtilities.TestBag;
import org.zim.gamsapi.TestUtilities.TestDigitalObject;
import org.zim.gamsapi.TestUtilities.TestIngestRecord;
import org.zim.gamsapi.UnitTest;

public class IngestRecordTest extends UnitTest {

    @Test
    public void getBaggingDateReturnsExpectedValue(){
        var TEST_INGEST_RECORD = TestIngestRecord.generate(TestDigitalObject.generate());
        Assertions.assertThat(TEST_INGEST_RECORD
                .getBaggingDate()).isEqualTo(TestBag.TestBagInfo.BAGGING_DATE);
    }

    @Test
    public void getBaggingTimeReturnsExpectedValue(){
        var TEST_INGEST_RECORD = TestIngestRecord.generate(TestDigitalObject.generate());
        Assertions.assertThat(TEST_INGEST_RECORD
                .getBaggingTime()).isEqualTo(TestBag.TestBagInfo.BAGGING_TIME);
    }

}
