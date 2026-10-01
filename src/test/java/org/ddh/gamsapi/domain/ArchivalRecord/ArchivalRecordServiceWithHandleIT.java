package org.ddh.gamsapi.domain.ArchivalRecord;

import org.assertj.core.api.Assertions;
import org.ddh.gamsapi.TestUtilities.TestDataBuilder;
import org.ddh.gamsapi.TestUtilities.TestDataSet;
import org.ddh.gamsapi.domain.ArchivalRecord.utils.handle.Handle;
import org.ddh.gamsapi.domain.ArchivalRecord.utils.handle.HandleAlreadyExistsException;
import org.ddh.gamsapi.domain.ArchivalRecord.utils.handle.HandleGenerator;
import org.ddh.gamsapi.domain.DigitalObject.utils.interfaces.IDigitalObjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.auditing.AuditingHandler;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import java.net.URI;

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

  @Nested
  class ReserveArchivalRecordByPid {

    @Test
    void doesNotCreateAHandleOnReservation(){

      var generatedHandle = handleGenerator.generate();

      var savedRecord = archivalRecordService.reserveArchivalRecordByPid(
          generatedHandle.toHdlUri()
      );

      Assertions.assertThat(
          archivalRecordRepository.existsById(savedRecord.getPid())
      ).isTrue();

      Assertions.assertThat(
          handleClient.exists(generatedHandle.toString()))
          .isFalse();

    }

    @Test
    void throwsIfGeneratedHandleAlreadyExists(){

      var generatedHandle = handleGenerator.generate();

      // first create handle on the server
      handleClient.register(generatedHandle.toString(), URI.create("https://google.at"));

      Assertions.assertThatThrownBy(
          () -> archivalRecordService.reserveArchivalRecordByPid(generatedHandle.toHdlUri())
      ).isInstanceOf(HandleAlreadyExistsException.class);

    }

  }

  @Nested
  class ReserveArchivalRecordByObjectId {

    @Test
    void createsExpectedArchivalRecord(){

      // make sure that test object has not test archival record
      archivalRecordRepository.deleteAll();

      var savedRecord = archivalRecordService.reserveArchivalRecordByObjectId(
          testDataSet.digitalObject().getId()
      );

      Assertions.assertThat(
          archivalRecordRepository.existsById(savedRecord.getPid())
      ).isTrue();

    }

    @Test
    void doesNotCreateHandleOnHandleServer(){

      // make sure that test object has not test archival record
      archivalRecordRepository.deleteAll();

      var savedRecord = archivalRecordService.reserveArchivalRecordByObjectId(
          testDataSet.digitalObject().getId()
      );

      var expectedHandle = Handle.parse(
          savedRecord.getPid()
      );

      Assertions.assertThat(
          handleClient.exists(expectedHandle.toString())
      ).isFalse();
    }

  }

}
