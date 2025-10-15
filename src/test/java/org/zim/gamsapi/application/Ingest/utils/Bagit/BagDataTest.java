package org.zim.gamsapi.application.Ingest.utils.Bagit;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.zim.gamsapi.TestUtilities.TestDatastream;
import org.zim.gamsapi.TestUtilities.TestDigitalObject;
import org.zim.gamsapi.TestUtilities.TestSubmissionRecord;
import org.zim.gamsapi.domain.Datastream.Datastream;
import org.zim.gamsapi.domain.DigitalObject.DigitalObject;
import org.zim.gamsapi.domain.DigitalObject.SubmissionRecord.SubmissionRecord;

import java.util.Set;

public class BagDataTest {

    @Nested
    public class From {

        final DigitalObject TEST_DIGITAL_OBJECT = TestDigitalObject.generate();
        final SubmissionRecord TEST_INGEST_RECORD = TestSubmissionRecord.generate(TEST_DIGITAL_OBJECT);
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

    @Nested
    public class ToSipJsonContent {
      final DigitalObject TEST_DIGITAL_OBJECT = TestDigitalObject.generate();
      final SubmissionRecord TEST_INGEST_RECORD = TestSubmissionRecord.generate(TEST_DIGITAL_OBJECT);
      final Datastream TEST_DATASTREAM = TestDatastream.generate(TEST_DIGITAL_OBJECT);

      BagData testBagdata;

      @BeforeEach
      public void beforeEach() {
          // Nothing to set up before each test in this case
          testBagdata = BagData.from(TEST_DIGITAL_OBJECT, Set.of(TEST_DATASTREAM), TEST_INGEST_RECORD);
      }

      @Test
      public void createsNoNullString() {
            var jsonString = testBagdata.toSipJsonContent();
            Assertions.assertThat(jsonString).isNotNull();
      }

      @Test
      public void createsNoEmptyString() {
            var jsonString = testBagdata.toSipJsonContent();
            Assertions.assertThat(jsonString).isNotEmpty();
      }

      @Test
      public void createdStringContainsExpectedValues() {
            var jsonString = testBagdata.toSipJsonContent();
            Assertions.assertThat(jsonString)
                .contains(
                    testBagdata.getId(),
                    testBagdata.getProject(),
                    testBagdata.getTitle(),
                    testBagdata.getObjectType(),
                    testBagdata.getDescription(),
                    testBagdata.getCreator(),
                    testBagdata.getRights(),
                    testBagdata.getPublisher(),
                    testBagdata.getFunder(),
                    testBagdata.getMainResource(),
                    testBagdata.getSchema(),
                    testBagdata.getCreatedBy(),
                    testBagdata.getSource()
                    );

            // Assert existence of collection values
            testBagdata.getContentFiles().forEach(bagFile -> {
                Assertions.assertThat(jsonString).contains(bagFile.getDsid());
            });
      }

      @Test
      public void createdStringDoesNotContainChecksums() {
        var jsonString = testBagdata.toSipJsonContent();
        Assertions.assertThat(jsonString)
            .doesNotContain(
                testBagdata.getMd5Checksum(),
                testBagdata.getSha512Checksum()
            );
      }

    }

}
