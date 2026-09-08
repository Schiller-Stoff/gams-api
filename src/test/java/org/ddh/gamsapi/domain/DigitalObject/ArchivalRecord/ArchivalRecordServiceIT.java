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
  class Generate {

    @Test
    void successfullySavesExpectedAdditionalArchivalRecord(){

      ArchivalRecordReserveDto archivalRecordReserveDto = new ArchivalRecordReserveDto();
      archivalRecordReserveDto.setDigitalObjectId(testDataSet.digitalObject().getId());
      archivalRecordReserveDto.setPid(testDataSet.archivalRecord().getPid());
      archivalRecordReserveDto.setTimeStamp(Instant.now());
      archivalRecordReserveDto.setExternalId(testDataSet.archivalRecord().getExternalId());
      archivalRecordService.reserve(archivalRecordReserveDto);

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
