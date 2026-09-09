package org.ddh.gamsapi.domain.DigitalObject.ArchivalRecord;

import org.assertj.core.api.Assertions;
import org.ddh.gamsapi.IntegrationTest;
import org.ddh.gamsapi.TestUtilities.TestArchivalRecord;
import org.ddh.gamsapi.TestUtilities.TestDataBuilder;
import org.ddh.gamsapi.TestUtilities.TestDataSet;
import org.ddh.gamsapi.TestUtilities.TestProject;
import org.ddh.gamsapi.domain.DigitalObject.utils.exceptions.DigitalObjectNotFoundException;
import org.hibernate.query.common.TemporalUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.auditing.AuditingHandler;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ArchivalRecordServiceIT extends IntegrationTest {

  @Autowired
  IArchivalRecordService archivalRecordService;

  @Autowired
  IArchivalRecordRepository archivalRecordRepository;

  // Deactivates the auditing process.
  @MockitoBean
  private AuditingHandler auditingHandler;

  @Autowired
  private TestDataBuilder testDataBuilder;

  private TestDataSet testDataSet;

  @BeforeEach
  void setup() {
    testDataSet = testDataBuilder.buildTestDataSet();
  }

  @Nested
  class FindForObject {

    @Test
    void foundArchivalRecordIsNotNullAndIsNotEmpty(){
      var foundRecord = archivalRecordService.findForObject(testDataSet.digitalObject().getId());
      Assertions.assertThat(foundRecord).isNotNull();
      Assertions.assertThat(foundRecord).isNotEmpty();
      Assertions.assertThat(foundRecord).hasSize(1);
    }

    @Test
    void findsExpectedArchivalRecord(){
      var expectedRecords = archivalRecordService.findForObject(testDataSet.digitalObject().getId());
      Assertions.assertThat(
          expectedRecords.getFirst().getId()
      ).isEqualTo(testDataSet.archivalRecord().getId());
    }

  }


  @Nested
  class Reserve {

    @Test
    void successfullySavesExpectedAdditionalArchivalRecord(){

      archivalRecordRepository.deleteAll();

      ArchivalRecordReserveDto archivalRecordReserveDto = new ArchivalRecordReserveDto();
      archivalRecordReserveDto.setPid(testDataSet.archivalRecord().getPid());
      archivalRecordReserveDto.setTimeStamp(Instant.now());
      archivalRecordReserveDto.setExternalId(testDataSet.archivalRecord().getExternalId());
      archivalRecordService.reserve(testDataSet.digitalObject().getId(), archivalRecordReserveDto);

      var foundRecords = archivalRecordService.findForObject(testDataSet.digitalObject().getId());

      // archival records count should still be one
      Assertions.assertThat(foundRecords).hasSize(1);

    }

    @Test
    void cannotSaveIfBlockingArchivalRecordExists(){

      ArchivalRecordReserveDto archivalRecordReserveDto = new ArchivalRecordReserveDto();
      archivalRecordReserveDto.setPid(testDataSet.archivalRecord().getPid());
      archivalRecordReserveDto.setTimeStamp(Instant.now());
      archivalRecordReserveDto.setExternalId(testDataSet.archivalRecord().getExternalId());

      // this will throw because testdata creates drafted archival record
      Assertions.assertThatThrownBy(() -> archivalRecordService.reserve(testDataSet.digitalObject().getId(), archivalRecordReserveDto))
          .isInstanceOf(ArchivalRecordAlreadyActiveException.class);


    }

  }

  @Nested
  class Draft {

    @Test
    void updatesArchivalRecordToExpectedValues(){

      final String TEST_EXTERNAL_ID = "fooBar";
      final String TEST_PID = "10.5281/zenodo.22658867";
      final Instant TEST_TIME = Instant.now();

      ArchivalRecordDraftDto archivalRecordDraftDto = new ArchivalRecordDraftDto();
      archivalRecordDraftDto.setPid(TEST_PID);
      archivalRecordDraftDto.setExternalId(TEST_EXTERNAL_ID);
      archivalRecordDraftDto.setTimeStamp(TEST_TIME);

      archivalRecordService.draftArchivalRecord(
          testDataSet.digitalObject().getId(),
          archivalRecordDraftDto
      );

      var recordOptional =  archivalRecordRepository.findById(testDataSet.archivalRecord().getId());

      Assertions.assertThat(recordOptional).isNotNull();
      Assertions.assertThat(recordOptional).isNotEmpty();

      var foundRecord = recordOptional.get();

      Assertions.assertThat(foundRecord.getId())
          .isEqualTo(testDataSet.archivalRecord().getId());

      Assertions.assertThat(foundRecord.getArchivingStatus())
              .isEqualTo(ArchivingStatus.DRAFTED);

      Assertions.assertThat(foundRecord.getPid())
          .isEqualTo(TEST_PID);

      Assertions.assertThat(foundRecord.getTimeStamp().truncatedTo(ChronoUnit.SECONDS)) // truncate because database doesn't save as exactly
          .isEqualTo(TEST_TIME.truncatedTo(ChronoUnit.SECONDS));

      Assertions.assertThat(foundRecord.getExternalId())
          .isEqualTo(TEST_EXTERNAL_ID);


    }

    @Test
    void throwsDigitalObjectNotFound(){

      final String NON_EXISTENT_OBJECT_ID = TestProject.PROJECT_ABBR + ".foo";

      final String TEST_EXTERNAL_ID = "fooBar";
      final String TEST_PID = "10.5281/zenodo.22658867";
      final Instant TEST_TIME = Instant.MIN;

      ArchivalRecordDraftDto archivalRecordDraftDto = new ArchivalRecordDraftDto();
      archivalRecordDraftDto.setPid(TEST_PID);
      archivalRecordDraftDto.setExternalId(TEST_EXTERNAL_ID);
      archivalRecordDraftDto.setTimeStamp(TEST_TIME);

      Assertions.assertThatThrownBy(() -> archivalRecordService.draftArchivalRecord(
              NON_EXISTENT_OBJECT_ID,
              archivalRecordDraftDto
          )).isInstanceOf(DigitalObjectNotFoundException.class);
      ;

    }

    @Test
    void throwsIfNoActiveArchivalRecordIsAvailable(){

      // set status of test record to PUBLISHED -> so non draft can be found.
      var testRecord = archivalRecordRepository.findById(testDataSet.archivalRecord().getId()).orElseThrow();
      testRecord.setArchivingStatus(ArchivingStatus.PUBLISHED);
      archivalRecordRepository.save(testRecord);

      // try to draft new record
      ArchivalRecordDraftDto archivalRecordDraftDto = new ArchivalRecordDraftDto();
      archivalRecordDraftDto.setPid(testRecord.getPid());
      archivalRecordDraftDto.setExternalId(testRecord.getExternalId());
      archivalRecordDraftDto.setTimeStamp(testRecord.getTimeStamp());

      Assertions.assertThatThrownBy(() -> archivalRecordService.draftArchivalRecord(
          testDataSet.digitalObject().getId(),
          archivalRecordDraftDto)
          )
          .isInstanceOf(ArchivalRecordNoActiveRecordException.class);

    }

    @Test
    void throwsIfNoArchivalRecordIsAvailable(){

      archivalRecordRepository.deleteAll();

      // try to draft new record
      ArchivalRecordDraftDto archivalRecordDraftDto = new ArchivalRecordDraftDto();
      archivalRecordDraftDto.setPid(TestArchivalRecord.PID);
      archivalRecordDraftDto.setExternalId(TestArchivalRecord.EXTERNAL_ID);
      archivalRecordDraftDto.setTimeStamp(TestArchivalRecord.TIME_STAMP);

      Assertions.assertThatThrownBy(() -> archivalRecordService.draftArchivalRecord(
              testDataSet.digitalObject().getId(),
              archivalRecordDraftDto)
          )
          .isInstanceOf(ArchivalRecordNoActiveRecordException.class);

    }

  }

  @Nested
  class Delete {

    @Test
    void successfullyDeletesExpectedArchivalRecord(){
      archivalRecordService.deleteById(testDataSet.archivalRecord().getId());
      var expectedDeleted = archivalRecordRepository.findById(testDataSet.archivalRecord().getId());
      Assertions.assertThat(expectedDeleted).isEmpty();
    }

  }

}
