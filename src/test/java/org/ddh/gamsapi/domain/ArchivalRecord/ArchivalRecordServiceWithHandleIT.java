package org.ddh.gamsapi.domain.ArchivalRecord;

import org.assertj.core.api.Assertions;
import org.ddh.gamsapi.TestUtilities.TestDataBuilder;
import org.ddh.gamsapi.TestUtilities.TestDataSet;
import org.ddh.gamsapi.domain.ArchivalRecord.utils.handle.Handle;
import org.ddh.gamsapi.domain.ArchivalRecord.utils.handle.HandleGenerator;
import org.ddh.gamsapi.domain.DigitalObject.utils.interfaces.IDigitalObjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.auditing.AuditingHandler;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class ArchivalRecordServiceWithHandleIT extends HandleIntegrationTest {

  @Autowired
  IArchivalRecordService archivalRecordService;

  @Autowired
  IArchivalRecordRepository archivalRecordRepository;

  @Autowired
  IDigitalObjectRepository  digitalObjectRepository;

  @Autowired
  private HandleGenerator handleGenerator;

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
  class ReserveArchivalRecord {

    @Test
    void createsExpectedArchivalRecord(){

      var savedRecord = archivalRecordService.reserveArchivalRecord();

      Assertions.assertThat(
          archivalRecordRepository.existsById(savedRecord.getPid())
      ).isTrue();

    }

    @Test
    void doesNotCreateHandleOnHandleServer(){
      var savedRecord = archivalRecordService.reserveArchivalRecord();

      var expectedHandle = Handle.parse(
          savedRecord.getPid()
      );

      Assertions.assertThat(
          handleClient.exists(expectedHandle.toString())
      ).isFalse();
    }

  }

}
