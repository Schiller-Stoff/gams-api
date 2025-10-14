package org.zim.gamsapi.application.Ingest.utils.Bagit;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.zim.gamsapi.TestUtilities.TestBag;
import org.zim.gamsapi.domain.DigitalObject.SubmissionRecord.SubmissionRecord;
import org.zim.gamsapi.TestUtilities.TestDigitalObject;
import org.zim.gamsapi.TestUtilities.TestSubmissionRecord;
import org.zim.gamsapi.UnitTest;

public class BagMetaTest extends UnitTest {

    @Nested
    public class From {

        final SubmissionRecord TEST_INGEST_RECORD = TestSubmissionRecord.generate(TestDigitalObject.generate());

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

    @Nested
    public class ToBagItTxtContent {

        final BagMeta TEST_BAG_META = BagMeta.builder()
            .bagItVersion(TestBag.BagitTxt.BAGIT_VERSION)
            .tagFileCharacterEncoding(TestBag.BagitTxt.TAG_FILE_CHARACTER_ENCODING)
            .build();

        @Test
        public void toBagMetaContentCreatesNoNullString(){
            String content = TEST_BAG_META.toBagItTxtContent();
            Assertions.assertThat(content).isNotNull();
        }

        @Test
        public void toBagMetaContentCreatesNonEmptyString(){
            String content = TEST_BAG_META.toBagItTxtContent();
            Assertions.assertThat(content).isNotEmpty();
        }

        @Test
        public void toBagMetaContentContainsExpectedValues(){
            String content = TEST_BAG_META.toBagItTxtContent();
            Assertions.assertThat(content)
                .contains(TestBag.BagitTxt.BAGIT_VERSION)
                .contains(TestBag.BagitTxt.TAG_FILE_CHARACTER_ENCODING);
        }

    }

}
