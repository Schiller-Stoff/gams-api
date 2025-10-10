package org.zim.gamsapi.application.Ingest.utils.Bagit;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.zim.gamsapi.domain.DigitalObject.SubmissionRecord.SubmissionRecord;
import org.zim.gamsapi.TestUtilities.TestDigitalObject;
import org.zim.gamsapi.TestUtilities.TestIngestRecord;
import org.zim.gamsapi.UnitTest;

public class BagMetaTest extends UnitTest {

    @Nested
    public class From {

        final SubmissionRecord TEST_INGEST_RECORD = TestIngestRecord.generate(TestDigitalObject.generate());

        @Test
        public void fromIngestRecordCreatesNoNullObject(){
            BagMeta bagMeta = BagMeta.from(TEST_INGEST_RECORD);
            Assertions.assertThat(bagMeta).isNotNull();
        }

        @Test
        public void fomIngestRecordCreatesNoNullFields(){
            BagMeta bagMeta = BagMeta.from(TEST_INGEST_RECORD);
            Assertions.assertThat(bagMeta).hasNoNullFieldsOrProperties();
        }

        @Test
        public void fromIngestRecordCreatesObjectWithExpectedValues(){
            BagMeta bagMeta = BagMeta.from(TEST_INGEST_RECORD);
            Assertions.assertThat(bagMeta.getBagItVersion()).isEqualTo(TEST_INGEST_RECORD.getBagVersion());
            Assertions.assertThat(bagMeta.getTagFileCharacterEncoding()).isEqualTo(TEST_INGEST_RECORD.getBagTagFileCharacterEncoding());
        }

    }

}
