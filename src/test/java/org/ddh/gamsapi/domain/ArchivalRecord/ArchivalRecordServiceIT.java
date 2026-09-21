package org.ddh.gamsapi.domain.ArchivalRecord;

import org.assertj.core.api.Assertions;
import org.ddh.gamsapi.IntegrationTest;
import org.ddh.gamsapi.TestUtilities.TestDataBuilder;
import org.ddh.gamsapi.TestUtilities.TestDataSet;
import org.ddh.gamsapi.domain.ArchivalRecord.utils.ArchivalState;
import org.ddh.gamsapi.domain.ArchivalRecord.utils.HandleGenerator;
import org.ddh.gamsapi.domain.ArchivalRecord.utils.dto.ArchivalRecordDraftDto;
import org.ddh.gamsapi.domain.ArchivalRecord.utils.dto.ArchivalRecordPublishDto;
import org.ddh.gamsapi.domain.DigitalObject.utils.exceptions.DigitalObjectNotFoundException;
import org.ddh.gamsapi.domain.DigitalObject.utils.interfaces.IDigitalObjectRepository;
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

  @Autowired
  IDigitalObjectRepository  digitalObjectRepository;

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
    void throwsIfDigitalObjectDoesNotExist(){
      final String NON_EXISTENT_OBJECT_ID = testDataSet.project().getProjectAbbr() + ".foobar";
      Assertions.assertThatThrownBy(() -> archivalRecordService.findForObject(NON_EXISTENT_OBJECT_ID))
          .isInstanceOf(
              DigitalObjectNotFoundException.class
          );
    }

  }

  @Nested
  class DeleteById {

    @Test
    void successfullyDeletesExpectedArchivalRecord(){
      archivalRecordRepository.deleteById(testDataSet.archivalRecord().getPid());
      Assertions.assertThat(
          archivalRecordRepository.existsById(testDataSet.archivalRecord().getPid())
      ).isFalse();
    }

  }

  @Nested
  class CreateArchivalRecord {

    @Test
    void createsExpectedArchivalRecord(){

      var savedRecord = archivalRecordService.createArchivalRecord();

      Assertions.assertThat(
          archivalRecordRepository.existsById(savedRecord.getPid())
      ).isTrue();

    }

  }

  @Nested
  class CreateArchivalRecordByPid {

    @Test
    void createsExpectedArchivalRecord(){

      final String TEST_PID = HandleGenerator.generate();

      var savedRecord = archivalRecordService.createArchivalRecordByPid(TEST_PID);

      Assertions.assertThat(
          archivalRecordRepository.existsById(savedRecord.getPid()))
          .isTrue();

    }

    @Test
    void throwsIfPidAlreadyExists(){

      final String ALREADY_EXISTING_PID = testDataSet.archivalRecord().getPid();

      Assertions.assertThatThrownBy(
          () -> archivalRecordService.createArchivalRecordByPid(ALREADY_EXISTING_PID)
      ).isInstanceOf(ArchivalRecordAlreadyExistsException.class);

    }

  }

  @Nested
  class CreateArchivalRecordByObjectId {

    @Test
    void createsExpectedArchivalRecord(){

      // make sure that test archival record is deleted (otherwise clash)
      archivalRecordRepository.deleteAll();

      var savedArchivalRecord = archivalRecordService.createArchivalRecordByObjectId(testDataSet.digitalObject().getId());
      var foundArchivalRecord = archivalRecordRepository.findById(savedArchivalRecord.getPid()).orElseThrow();

      Assertions.assertThat(savedArchivalRecord)
          .isNotNull();

      Assertions.assertThat(savedArchivalRecord.getPid())
          .isEqualTo(foundArchivalRecord.getPid());


    }

    @Test
    void doesNotChangeLinkedDigitalObject(){

      // make sure that test archival record is deleted (otherwise clash)
      archivalRecordRepository.deleteAll();

      archivalRecordService.createArchivalRecordByObjectId(testDataSet.digitalObject().getId());

      var linkedObject = digitalObjectRepository.findDigitalObjectById(testDataSet.digitalObject().getId())
          .orElseThrow();

      Assertions.assertThat(linkedObject.getPublisher())
          .isNotNull()
          .isEqualTo(testDataSet.digitalObject().getPublisher());

      Assertions.assertThat(linkedObject.getObjectType())
          .isEqualTo(testDataSet.digitalObject().getObjectType());

    }

    @Test
    void throwsIfActiveArchivalRecordAlreadyExists(){

      // test saved archival record already exists
      Assertions.assertThatThrownBy(() -> {
        archivalRecordService.createArchivalRecordByObjectId(testDataSet.digitalObject().getId());
      }).isInstanceOf(
          ArchivalRecordAlreadyActiveException.class
      );

    }

    @Test
    void throwsIfDigitalObjectWasNotFound(){

      final String NON_EXISTENT_OBJECT_ID = testDataSet.project().getProjectAbbr() + ".foobar";

      Assertions.assertThatThrownBy(() -> archivalRecordService.createArchivalRecordByObjectId(NON_EXISTENT_OBJECT_ID))
          .isInstanceOf(DigitalObjectNotFoundException.class);

    }


  }

  @Nested
  class CreateArchivalRecordByObjectIdAndPid {

    @Test
    void createsExpectedArchivalRecordViaProvidingExternalPid(){

      final String TEST_PID = HandleGenerator.generate();

      var savedArchivalRecord = archivalRecordService.createArchivalRecordByObjectIdAndPid(testDataSet.digitalObject().getId(), TEST_PID);
      var foundArchivalRecord = archivalRecordRepository.findById(savedArchivalRecord.getPid()).orElseThrow();

      Assertions.assertThat(savedArchivalRecord)
          .isNotNull();

      Assertions.assertThat(savedArchivalRecord.getPid())
          .isEqualTo(foundArchivalRecord.getPid());


    }

    @Test
    void throwsIfDigitalObjectWasNotFound(){

      final String TEST_PID = HandleGenerator.generate();
      final String NON_EXISTENT_OBJECT_ID = testDataSet.project().getProjectAbbr() + ".foobar";

      Assertions.assertThatThrownBy(() -> archivalRecordService.createArchivalRecordByObjectIdAndPid(NON_EXISTENT_OBJECT_ID, TEST_PID))
          .isInstanceOf(DigitalObjectNotFoundException.class);

    }

    @Test
    void throwsIfPidAlreadyExists(){

      final String ALREADY_EXISTING_PID = testDataSet.archivalRecord().getPid();
      Assertions.assertThatThrownBy(() -> archivalRecordService.createArchivalRecordByObjectIdAndPid(testDataSet.digitalObject().getId(), ALREADY_EXISTING_PID))
          .isInstanceOf(ArchivalRecordAlreadyExistsException.class);
    }

  }

  @Nested
  class SaveArchivalRecord {

    @Test
    void changesExpectedArchivalRecord(){

      final String TEST_EXTERNAL_REFERENCE = "foobarxyz";
      final Instant TEST_TIMESTAMP = Instant.now();

      var changeDto = new ArchivalRecordDto();
      changeDto.setPid(testDataSet.archivalRecord().getPid());
      changeDto.setExternalId(TEST_EXTERNAL_REFERENCE);
      changeDto.setPublicationTimeStamp(TEST_TIMESTAMP);

      var savedRecord = archivalRecordService.saveArchivalRecord(changeDto);

      Assertions.assertThat(savedRecord.getExternalId()).isEqualTo(TEST_EXTERNAL_REFERENCE);
      Assertions.assertThat(savedRecord.getPublicationTimeStamp()).isEqualTo(TEST_TIMESTAMP);

    }

    @Test
    void throwsIfArchivalRecordDoesNotExist(){

      final String TEST_PID = HandleGenerator.generate();

      var changeDto = new ArchivalRecordDto();
      changeDto.setPid(TEST_PID);
      changeDto.setExternalId("foobarxyz");
      changeDto.setPublicationTimeStamp(Instant.now());

      Assertions.assertThatThrownBy(() -> archivalRecordService.saveArchivalRecord(changeDto))
          .isInstanceOf(ArchivalRecordNotFoundException.class);

    }

    @Test
    void throwsIfNeitherPublicationTimeNorExternalIdWasGiven(){

      var changeDto = new ArchivalRecordDto();
      changeDto.setPid(testDataSet.archivalRecord().getPid());

      Assertions.assertThatThrownBy(() -> archivalRecordService.saveArchivalRecord(changeDto))
      .isInstanceOf(ArchivalRecordInvalidStateException.class);

    }

    @Test
    void cannotNullifyArchivalRecordExternalIdIfPublicationTimestampIsAlreadySet(){

      // need to make sure that a publication timestamp was defined first
      var testRecord = archivalRecordRepository.findById(testDataSet.archivalRecord().getPid()).orElseThrow();
      testRecord.setPublicationTimeStamp(Instant.now());
      archivalRecordRepository.save(testRecord);

      var changeDto = new ArchivalRecordDto();
      changeDto.setPid(testDataSet.archivalRecord().getPid());
      changeDto.setExternalId(null);
      // publication timestamp must be null here

      Assertions.assertThatThrownBy(() -> archivalRecordService.saveArchivalRecord(changeDto))
          .isInstanceOf(ArchivalRecordInvalidStateException.class);

    }

    @Test
    void cannotSetPublicationTimeStampToNull(){

      Instant TEST_PUBLICATION_TIMESTAMP = Instant.now().truncatedTo(ChronoUnit.SECONDS);

      // need to make sure that a publication timestamp was defined first
      var testRecord = archivalRecordRepository.findById(testDataSet.archivalRecord().getPid()).orElseThrow();
      testRecord.setPublicationTimeStamp(TEST_PUBLICATION_TIMESTAMP);
      testRecord.setExternalId("foobarxyz");
      archivalRecordRepository.save(testRecord);

      var changeDto = new ArchivalRecordDto();
      changeDto.setPid(testDataSet.archivalRecord().getPid());
      changeDto.setExternalId(testRecord.getExternalId());
      // publication timestamp must be null
      changeDto.setPublicationTimeStamp(null);

      var savedRecord = archivalRecordService.saveArchivalRecord(changeDto);

      // publicationTimestamp not null
      Assertions.assertThat(savedRecord.getPublicationTimeStamp()).isEqualTo(TEST_PUBLICATION_TIMESTAMP.truncatedTo(ChronoUnit.SECONDS));

    }

  }

  @Nested
  class DraftArchivalRecord {

    @Test
    void changesExpectedArchivalRecordExternalReference(){

      final String TEST_EXTERNAL_REFERENCE = "foobarxyz";

      var changeDto = new ArchivalRecordDraftDto();
      changeDto.setExternalId(TEST_EXTERNAL_REFERENCE);

      var changedRecord = archivalRecordService.draftArchivalRecord(
          testDataSet.archivalRecord().getPid(),
          changeDto
      );

      Assertions.assertThat(changedRecord.getExternalId())
          .isEqualTo(TEST_EXTERNAL_REFERENCE);

      Assertions.assertThat(changedRecord.getArchivalState())
          .isEqualTo(ArchivalState.DRAFT);

    }

    @Test
    void throwsIfProvidedArchivalRecordExternalIdIsNull(){
      var changeDto = new ArchivalRecordDraftDto();
      changeDto.setExternalId(null);

      Assertions.assertThatThrownBy(() -> archivalRecordService.draftArchivalRecord(
          testDataSet.archivalRecord().getPid(),
          changeDto
      )).isInstanceOf(
          ArchivalRecordInvalidStateException.class
      );

    }

    @Test
    void throwsIfArchivalRecordDoesNotExist(){
      final String NOT_EXISTENT_TEST_PID = testDataSet.archivalRecord().getPid().replace("1", "9");
      var changeDto = new ArchivalRecordDraftDto();
      changeDto.setExternalId(testDataSet.archivalRecord().getExternalId());

      Assertions.assertThatThrownBy(() -> archivalRecordService.draftArchivalRecord(
          NOT_EXISTENT_TEST_PID,
          changeDto
      )).isInstanceOf(
          ArchivalRecordNotFoundException.class
      );

    }

    @Test
    void throwsIfArchivalRecordStateIsNotValid(){

      // first change state of test archival record
      var archivalRecord = archivalRecordRepository.findById(testDataSet.archivalRecord().getPid()).orElseThrow();

      archivalRecord.setArchivalState(ArchivalState.PUBLISHED);
      archivalRecord.setExternalId(testDataSet.archivalRecord().getExternalId());

      archivalRecordRepository.save(archivalRecord);

      var changeDto = new ArchivalRecordDraftDto();
      changeDto.setExternalId(testDataSet.archivalRecord().getExternalId());

      Assertions.assertThatThrownBy(() -> archivalRecordService.draftArchivalRecord(
          testDataSet.archivalRecord().getPid(),
          changeDto
      )).isInstanceOf(ArchivalRecordInvalidStateException.class);

    }



  }

  @Nested
  class PublishArchivalRecord {

    @Test
    void publishesExpectedArchivalRecord(){

      // first need to get test data to draft state
      var foundArchivalRecord = archivalRecordRepository.findById(testDataSet.archivalRecord().getPid()).orElseThrow();

      final String TEST_EXTERNAL_ID = "foobarxyz";

      foundArchivalRecord.setArchivalState(ArchivalState.DRAFT);
      foundArchivalRecord.setExternalId(TEST_EXTERNAL_ID);
      archivalRecordRepository.save(foundArchivalRecord);

      final Instant TEST_PUBLICATION_TIMESTAMP = Instant.now().truncatedTo(ChronoUnit.SECONDS);

      ArchivalRecordPublishDto changeDto = new ArchivalRecordPublishDto();
      changeDto.setPublicationTimeStamp(TEST_PUBLICATION_TIMESTAMP);

      var publishedArchivalRecord = archivalRecordService.publishArchivalRecord(
          testDataSet.archivalRecord().getPid(),
          changeDto
      );

      Assertions.assertThat(
          publishedArchivalRecord.getPublicationTimeStamp()
      ).isEqualTo(TEST_PUBLICATION_TIMESTAMP);

      Assertions.assertThat(publishedArchivalRecord.getArchivalState())
          .isEqualTo(ArchivalState.PUBLISHED);

      Assertions.assertThat(publishedArchivalRecord.getExternalId())
          .isEqualTo(TEST_EXTERNAL_ID);
    }

    @Test
    void throwsIfPublicationDateIsNull(){

      var changeDto = new ArchivalRecordPublishDto();
      changeDto.setPublicationTimeStamp(null);

      Assertions.assertThatThrownBy(
          () -> archivalRecordService.publishArchivalRecord(testDataSet.archivalRecord().getPid(), changeDto)
      ).isInstanceOf(ArchivalRecordInvalidPublicationTimeStampException.class);

    }

    @Test
    void throwsIfArchivalRecordWasNotFound(){

      var changeDto = new ArchivalRecordPublishDto();
      changeDto.setPublicationTimeStamp(Instant.now().truncatedTo(ChronoUnit.SECONDS));

      final String NOT_EXISTING_PID = testDataSet.archivalRecord().getPid().replace("1", "9");

      Assertions.assertThatThrownBy(
          () -> archivalRecordService.publishArchivalRecord(NOT_EXISTING_PID, changeDto)
      ).isInstanceOf(ArchivalRecordNotFoundException.class);

    }

    @Test
    void cannotPublishRecordInRESERVEDState(){

      // first change test data
      var testArchivalRecord = archivalRecordRepository.findById(testDataSet.archivalRecord().getPid()).orElseThrow();
      testArchivalRecord.setArchivalState(ArchivalState.RESERVED);
      testArchivalRecord.setExternalId("foobarxyz");
      archivalRecordRepository.save(testArchivalRecord);

      var changeDto = new ArchivalRecordPublishDto();
      changeDto.setPublicationTimeStamp(Instant.now().truncatedTo(ChronoUnit.SECONDS));

      Assertions.assertThatThrownBy(
          () -> archivalRecordService.publishArchivalRecord(testArchivalRecord.getPid(), changeDto)
      ).isInstanceOf(ArchivalRecordInvalidStateException.class);



    }

    @Test
    void cannotPublishRecordInPUBLISHEDState(){

      // first change test data
      var testArchivalRecord = archivalRecordRepository.findById(testDataSet.archivalRecord().getPid()).orElseThrow();
      testArchivalRecord.setArchivalState(ArchivalState.PUBLISHED);
      testArchivalRecord.setExternalId("foobarxyz");
      testArchivalRecord.setPublicationTimeStamp(Instant.now().truncatedTo(ChronoUnit.SECONDS));
      archivalRecordRepository.save(testArchivalRecord);

      var changeDto = new ArchivalRecordPublishDto();
      changeDto.setPublicationTimeStamp(Instant.now().truncatedTo(ChronoUnit.SECONDS));

      Assertions.assertThatThrownBy(
          () -> archivalRecordService.publishArchivalRecord(testArchivalRecord.getPid(), changeDto)
      ).isInstanceOf(ArchivalRecordInvalidStateException.class);



    }

  }

}
