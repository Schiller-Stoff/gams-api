package org.zim.gamsapi.application.Ingest.utils.Bagit;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.zim.gamsapi.domain.Datastream.Datastream;
import org.zim.gamsapi.domain.DigitalObject.DigitalObject;
import org.zim.gamsapi.application.Ingest.SubmissionRecord;
import org.zim.gamsapi.TestUtilities.TestDatastream;
import org.zim.gamsapi.TestUtilities.TestDigitalObject;
import org.zim.gamsapi.TestUtilities.TestIngestRecord;
import java.util.Set;

public class BagDataTest {

    @Nested
    public class From {

        final DigitalObject TEST_DIGITAL_OBJECT = TestDigitalObject.generate();
        final SubmissionRecord TEST_INGEST_RECORD = TestIngestRecord.generate(TEST_DIGITAL_OBJECT);
        final Datastream TEST_DATASTREAM = TestDatastream.generate(TEST_DIGITAL_OBJECT);

        @Test
        public void fromCreatesNoNullBagData(){
            var testBagdata = BagData.from(TEST_DIGITAL_OBJECT, Set.of(TEST_DATASTREAM), TEST_INGEST_RECORD);
            Assertions.assertThat(testBagdata).isNotNull();
        }

        @Test
        public void fromCreatesNoNullFieldsBagDataObject(){
            var testBagdata = BagData.from(TEST_DIGITAL_OBJECT, Set.of(TEST_DATASTREAM), TEST_INGEST_RECORD);
            Assertions.assertThat(testBagdata).hasNoNullFieldsOrProperties();
        }

        @Test
        public void fromCreatesExpectedBagDataObject(){
            var testBagdata = BagData.from(TEST_DIGITAL_OBJECT, Set.of(TEST_DATASTREAM), TEST_INGEST_RECORD);
            Assertions.assertThat(testBagdata.getId()).isEqualTo(TEST_DIGITAL_OBJECT.getId());
            Assertions.assertThat(testBagdata.getProject()).isEqualTo(TEST_DIGITAL_OBJECT.getProject().getProjectAbbr());
            Assertions.assertThat(testBagdata.getTitle()).isEqualTo(TEST_DIGITAL_OBJECT.getBaseMetadata().getTitle());
            Assertions.assertThat(testBagdata.getObjectType()).isEqualTo(TEST_DIGITAL_OBJECT.getObjectType());
            Assertions.assertThat(testBagdata.getDescription()).isEqualTo(TEST_DIGITAL_OBJECT.getBaseMetadata().getDescription());
            Assertions.assertThat(testBagdata.getCreator()).isEqualTo(TEST_DIGITAL_OBJECT.getBaseMetadata().getCreator());
            Assertions.assertThat(testBagdata.getRights()).isEqualTo(TEST_DIGITAL_OBJECT.getBaseMetadata().getRights());
            Assertions.assertThat(testBagdata.getPublisher()).isEqualTo(TEST_DIGITAL_OBJECT.getPublisher());
            Assertions.assertThat(testBagdata.getFunder()).isEqualTo(TEST_DIGITAL_OBJECT.getFunder());
            Assertions.assertThat(testBagdata.getMainResource()).isEqualTo(TEST_DIGITAL_OBJECT.getMainResource());
            Assertions.assertThat(testBagdata.getContentFiles()).hasSize(1);
            Assertions.assertThat(testBagdata.getContentFiles().iterator().next().getDsid()).isEqualTo(TEST_DATASTREAM.getDsid());
            Assertions.assertThat(testBagdata.getMd5Checksum()).isEqualTo(TEST_DIGITAL_OBJECT.getBaseMetadata().getMd5Checksum());
            Assertions.assertThat(testBagdata.getSha512Checksum()).isEqualTo(TEST_DIGITAL_OBJECT.getBaseMetadata().getSha512Checksum());
            Assertions.assertThat(testBagdata.getSchema()).isEqualTo(TEST_INGEST_RECORD.getBagSchema());
            Assertions.assertThat(testBagdata.getCreatedBy()).isEqualTo(TEST_INGEST_RECORD.getBagCreatedBy());
            Assertions.assertThat(testBagdata.getSource()).isEqualTo(TEST_INGEST_RECORD.getBagSource());
        }

    }

}
