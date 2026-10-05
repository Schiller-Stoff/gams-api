package org.ddh.gamsapi.domain.ArchivalRecord;

import org.assertj.core.api.Assertions;
import org.ddh.gamsapi.TestUtilities.TestDataBuilder;
import org.ddh.gamsapi.TestUtilities.TestDataSet;
import org.ddh.gamsapi.domain.ArchivalRecord.utils.ArchivalState;
import org.ddh.gamsapi.domain.ArchivalRecord.utils.dto.ArchivalRecordDraftDto;
import org.ddh.gamsapi.domain.ArchivalRecord.utils.dto.ArchivalRecordPublishDto;
import org.ddh.gamsapi.domain.ArchivalRecord.utils.handle.Handle;
import org.ddh.gamsapi.domain.ArchivalRecord.utils.handle.HandleAlreadyExistsException;
import org.ddh.gamsapi.domain.ArchivalRecord.utils.handle.HandleGenerator;
import org.ddh.gamsapi.domain.ArchivalRecord.utils.handle.HandleNotRegisteredException;
import org.ddh.gamsapi.domain.DigitalObject.utils.interfaces.IDigitalObjectRepository;
import org.ddh.gamsapi.infrastructure.System.configproperties.HandleServerProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.auditing.AuditingHandler;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import java.net.URI;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

class ArchivalRecordServiceWithHandleIT extends HandleIntegrationTest {

  @Autowired
  IArchivalRecordService archivalRecordService;

  @Autowired
  IArchivalRecordRepository archivalRecordRepository;

  @Autowired
  HandleServerProperties  handleServerProperties;

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

  @Nested
  class ReserveArchivalRecordByObjectIdAndPid {

    @Test
    void createsExpectedArchivalRecord(){
      // make sure that test object has not test archival record
      archivalRecordRepository.deleteAll();

      var generatedHandle = handleGenerator.generate();

      var savedRecord = archivalRecordService.reserveArchivalRecordByObjectIdAndPid(
          testDataSet.digitalObject().getId(),
          generatedHandle.toHdlUri()
      );

      Assertions.assertThat(
          archivalRecordRepository.existsById(savedRecord.getPid()))
          .isTrue();

    }

    @Test
    void throwsIfGivenHandleAlreadyExists(){
      var generatedHandle = handleGenerator.generate();

      handleClient.register(generatedHandle.toString(), URI.create("https://google.at"));

      Assertions.assertThatThrownBy(() ->  archivalRecordService.reserveArchivalRecordByObjectIdAndPid(
          testDataSet.digitalObject().getId(),
          generatedHandle.toHdlUri()
      )).isInstanceOf(HandleAlreadyExistsException.class);

    }

  }

  @Nested
  class DraftArchivalRecord {

    @Test
    void createsExpectedHandleOnHandleServer(){

      var changeDto = new ArchivalRecordDraftDto();
      changeDto.setExternalId("foobarxyz");

      var changedRecord = archivalRecordService.draftArchivalRecord(
          testDataSet.archivalRecord().getPid(),
          changeDto
      );

      Assertions.assertThat(handleClient.exists(changedRecord.getPid()))
          .isTrue();

    }

    @Test
    void createsExpectedHandleTarget(){

      var changeDto = new ArchivalRecordDraftDto();
      changeDto.setExternalId("foobarxyz");

      var changedRecord = archivalRecordService.draftArchivalRecord(
          testDataSet.archivalRecord().getPid(),
          changeDto
      );

      final String EXPECTED_HANDLE_TARGET = String.format(
          "%s/api/curation/v1/projects/%s/objects/%s",
          handleServerProperties.getReserveBaseUrl(),
          testDataSet.project().getProjectAbbr(),
          testDataSet.digitalObject().getId()
      );

      Assertions.assertThat(handleClient.resolveTarget(changedRecord.getPid()).orElseThrow().toString())
          .isEqualTo(EXPECTED_HANDLE_TARGET);

    }

  }


  @Nested
  class PublishArchivalRecord {

    @Test
    void retargetsHandleToExpectedValue(){

      // first need to get test data to draft state
      var foundArchivalRecord = archivalRecordRepository.findById(
          testDataSet.archivalRecord().getPid()
      ).orElseThrow();

      final String TEST_EXTERNAL_ID = "foobarxyz";
      final URI TEST_ORIGINAL_TARGET = URI.create("https://google.at");

      foundArchivalRecord.setArchivalState(ArchivalState.DRAFT);
      foundArchivalRecord.setExternalId(TEST_EXTERNAL_ID);
      archivalRecordRepository.save(foundArchivalRecord);

      // create expected handle (would have been created in the DRAFT state)
      var associatedHandle = Handle.parse(foundArchivalRecord.getPid());
      handleClient.register(associatedHandle.toString(), TEST_ORIGINAL_TARGET);

      final Instant TEST_PUBLICATION_TIMESTAMP = Instant.now().truncatedTo(ChronoUnit.SECONDS);

      ArchivalRecordPublishDto changeDto = new ArchivalRecordPublishDto();
      changeDto.setPublicationTimeStamp(TEST_PUBLICATION_TIMESTAMP);

      // publish actual record
      archivalRecordService.publishArchivalRecord(
          foundArchivalRecord.getPid(),
          changeDto
      );

      final URI ACTUAL_HANDLE_TARGET = handleClient.resolveTarget(foundArchivalRecord.getPid())
          .orElseThrow();

      // must retarget to configured value
      final String EXPECTED_HANDLE_TARGET = handleServerProperties.getTargetBaseUrl() + "/" + TEST_EXTERNAL_ID;

      Assertions.assertThat(ACTUAL_HANDLE_TARGET.toString())
          .contains(TEST_EXTERNAL_ID)
          .contains(handleServerProperties.getTargetBaseUrl())
          .isEqualTo(EXPECTED_HANDLE_TARGET)
          .isNotEqualTo(TEST_ORIGINAL_TARGET.toString());

    }

    @Test
    void throwsIfExpectedHandleDoesNotExist(){
      // first need to get test data to draft state
      var foundArchivalRecord = archivalRecordRepository.findById(
          testDataSet.archivalRecord().getPid()
      ).orElseThrow();

      final String TEST_EXTERNAL_ID = "foobarxyz";

      foundArchivalRecord.setArchivalState(ArchivalState.DRAFT);
      foundArchivalRecord.setExternalId(TEST_EXTERNAL_ID);
      archivalRecordRepository.save(foundArchivalRecord);

      // publish actual record
      ArchivalRecordPublishDto changeDto = new ArchivalRecordPublishDto();
      changeDto.setPublicationTimeStamp(
          Instant.now().truncatedTo(ChronoUnit.SECONDS)
      );

      // skip registering of handle
      // make sure that handle does not exist
      var associatedHandle = Handle.parse(foundArchivalRecord.getPid());
      Assertions.assertThat(handleClient.exists(associatedHandle.toString()))
              .isFalse();

      Assertions.assertThatThrownBy(
          () -> archivalRecordService.publishArchivalRecord(
              foundArchivalRecord.getPid(),
              changeDto
          )
      ).isInstanceOf(HandleNotRegisteredException.class);


    }


  }

}
