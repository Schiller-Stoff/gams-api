package org.ddh.gamsapi.domain.DigitalObject.ArchivalRecord;

import org.assertj.core.api.Assertions;
import org.ddh.gamsapi.IntegrationTest;
import org.ddh.gamsapi.TestUtilities.TestArchivalRecord;
import org.ddh.gamsapi.TestUtilities.TestDataBuilder;
import org.ddh.gamsapi.TestUtilities.TestDataSet;
import org.ddh.gamsapi.TestUtilities.TestProject;
import org.ddh.gamsapi.domain.DigitalObject.utils.exceptions.DigitalObjectNotFoundException;
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
      archivalRecordService.reserve(testDataSet.digitalObject().getId(), archivalRecordReserveDto);

      var foundRecords = archivalRecordService.findForObject(testDataSet.digitalObject().getId());

      // archival records count should still be one
      Assertions.assertThat(foundRecords).hasSize(1);

    }

    @Test
    void cannotSaveIfBlockingArchivalRecordExists(){

      ArchivalRecordReserveDto archivalRecordReserveDto = new ArchivalRecordReserveDto();
      archivalRecordReserveDto.setPid(testDataSet.archivalRecord().getPid());

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
  class Publish {

    @Test
    void updatesArchivalRecordToExpectedValues(){

      // first delete test entry
      archivalRecordRepository.deleteAll();

      // create test record is DRAFT
      final String TEST_EXTERNAL_ID = "fooBar";
      final String TEST_PID = "10.5281/zenodo.22658867";

      ArchivalRecord archivalRecord = new ArchivalRecord();
      archivalRecord.setExternalId(TEST_EXTERNAL_ID);
      archivalRecord.setTimeStamp(Instant.now()); // truncated to different value
      archivalRecord.setPid(TEST_PID);
      archivalRecord.setArchivingStatus(ArchivingStatus.DRAFTED); // at first in drafted state
      archivalRecord.setDigitalObject(testDataSet.digitalObject());
      var savedTestArchivalRecord = archivalRecordRepository.save(archivalRecord);


      // publish archival record
      final Instant TEST_TIME = Instant.now();
      ArchivalRecordPublishDto  archivalRecordPublishDto = new ArchivalRecordPublishDto();
      archivalRecordPublishDto.setTimeStamp(TEST_TIME);
      archivalRecordService.publishArchivalRecord(testDataSet.digitalObject().getId(), archivalRecordPublishDto);


      // check if values are as expected
      var updatedTestArchivalRecordOptional =  archivalRecordRepository.findById(savedTestArchivalRecord.getId());

      Assertions.assertThat(updatedTestArchivalRecordOptional).isNotEmpty();

      var updatedTestArchivalRecord = updatedTestArchivalRecordOptional.get();

      Assertions.assertThat(updatedTestArchivalRecord.getId())
          .isEqualTo(savedTestArchivalRecord.getId());

      Assertions.assertThat(updatedTestArchivalRecord.getArchivingStatus())
          .isEqualTo(ArchivingStatus.PUBLISHED);

      Assertions.assertThat(updatedTestArchivalRecord.getPid())
          .isEqualTo(savedTestArchivalRecord.getPid());

      Assertions.assertThat(updatedTestArchivalRecord.getTimeStamp().truncatedTo(ChronoUnit.SECONDS)) // truncate because database doesn't save as exactly
          .isEqualTo(TEST_TIME.truncatedTo(ChronoUnit.SECONDS));

      Assertions.assertThat(updatedTestArchivalRecord.getExternalId())
          .isEqualTo(savedTestArchivalRecord.getExternalId());

    }

    @Test
    void throwsIfNoArchivalRecordsExist(){

      archivalRecordRepository.deleteAll();

      ArchivalRecordPublishDto  archivalRecordPublishDto = new ArchivalRecordPublishDto();
      archivalRecordPublishDto.setTimeStamp(Instant.now());

      Assertions.assertThatThrownBy(() -> {
        archivalRecordService.publishArchivalRecord(testDataSet.digitalObject().getId(), archivalRecordPublishDto);
      }).isInstanceOf(ArchivalRecordNoActiveRecordException.class);

    }

    @Test
    void throwsIfDigitalObjectDoesNotExist(){

      final String NOT_EXISTENT_OBJECT_ID = TestProject.PROJECT_ABBR + ".foobar";

      ArchivalRecordPublishDto  archivalRecordPublishDto = new ArchivalRecordPublishDto();
      archivalRecordPublishDto.setTimeStamp(Instant.now());

      Assertions.assertThatThrownBy(() -> {
        archivalRecordService.publishArchivalRecord(NOT_EXISTENT_OBJECT_ID, archivalRecordPublishDto);
      }).isInstanceOf(DigitalObjectNotFoundException.class);

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
