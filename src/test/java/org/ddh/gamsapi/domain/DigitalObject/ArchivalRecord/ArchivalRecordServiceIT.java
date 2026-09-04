package org.ddh.gamsapi.domain.DigitalObject.ArchivalRecord;

import org.assertj.core.api.Assertions;
import org.ddh.gamsapi.IntegrationTest;
import org.ddh.gamsapi.TestUtilities.TestDataBuilder;
import org.ddh.gamsapi.TestUtilities.TestDataSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.auditing.AuditingHandler;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.TransactionSystemException;

import java.time.Instant;

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
  class Save {

    @Test
    void failsToSaveAdditionalRecordWithoutArchivingStatus(){

      ArchivalRecordCreateDto archivalRecordCreateDto = new ArchivalRecordCreateDto();
      archivalRecordCreateDto.setDigitalObjectId(testDataSet.digitalObject().getId());
      archivalRecordCreateDto.setPid(testDataSet.archivalRecord().getPid());
      archivalRecordCreateDto.setTimeStamp(Instant.now());
      archivalRecordCreateDto.setExternalId(testDataSet.archivalRecord().getExternalId());
      // skip archiving status

      Assertions.assertThatThrownBy(
          () -> archivalRecordService.save(archivalRecordCreateDto)
      ).isInstanceOf(TransactionSystemException.class);

    }

    @Test
    void failsToSaveAdditionalRecordWithoutExternalId(){

      ArchivalRecordCreateDto archivalRecordCreateDto = new ArchivalRecordCreateDto();
      archivalRecordCreateDto.setDigitalObjectId(testDataSet.digitalObject().getId());
      archivalRecordCreateDto.setPid(testDataSet.archivalRecord().getPid());
      archivalRecordCreateDto.setTimeStamp(Instant.now());
      archivalRecordCreateDto.setArchivingStatus(testDataSet.archivalRecord().getArchivingStatus());
      // skip archiving status

      Assertions.assertThatThrownBy(
          () -> archivalRecordService.save(archivalRecordCreateDto)
      ).isInstanceOf(TransactionSystemException.class);

    }

    @Test
    void successfullySavesExpectedAdditionalArchivalRecord(){

      ArchivalRecordCreateDto archivalRecordCreateDto = new ArchivalRecordCreateDto();
      archivalRecordCreateDto.setDigitalObjectId(testDataSet.digitalObject().getId());
      archivalRecordCreateDto.setPid(testDataSet.archivalRecord().getPid());
      archivalRecordCreateDto.setTimeStamp(Instant.now());
      archivalRecordCreateDto.setArchivingStatus(testDataSet.archivalRecord().getArchivingStatus());
      archivalRecordCreateDto.setExternalId(testDataSet.archivalRecord().getExternalId());
      archivalRecordService.save(archivalRecordCreateDto);

      var foundRecords = archivalRecordService.findForObject(testDataSet.digitalObject().getId());

      Assertions.assertThat(foundRecords).hasSize(2);

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
