package org.ddh.gamsapi.domain.ArchivalRecord;

import org.assertj.core.api.Assertions;
import org.ddh.gamsapi.IntegrationTest;
import org.ddh.gamsapi.TestUtilities.TestDataBuilder;
import org.ddh.gamsapi.TestUtilities.TestDataSet;
import org.ddh.gamsapi.domain.ArchivalRecord.utils.ArchivalState;
import org.ddh.gamsapi.domain.ArchivalRecord.utils.HandleGenerator;
import org.ddh.gamsapi.domain.ArchivalRecord.utils.dto.ArchivalRecordDraftDto;
import org.ddh.gamsapi.domain.ArchivalRecord.utils.dto.ArchivalRecordPublishDto;
import org.ddh.gamsapi.domain.ArchivalRecord.utils.exceptions.ArchivalRecordInconsistentActiveRecordsException;
import org.ddh.gamsapi.domain.DigitalObject.DigitalObject;
import org.ddh.gamsapi.domain.DigitalObject.utils.exceptions.DigitalObjectNotFoundException;
import org.ddh.gamsapi.domain.DigitalObject.utils.interfaces.IDigitalObjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.auditing.AuditingHandler;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;

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
      var foundRecords = archivalRecordService.findForObject(testDataSet.digitalObject().getId());
      Assertions.assertThat(foundRecords)
          .isNotNull()
          .isNotEmpty()
          .hasSize(1);
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
  class FindArchivalRecordsForObject {

    @Test
    void findsExpectedPublishedArchivalRecords(){

      // use later to search only for published state
      final Set<ArchivalState> TEST_ARCHIVAL_STATES = Set.of(ArchivalState.PUBLISHED);

     // add another published archival record
      ArchivalRecord publishedRecord = new ArchivalRecord();
      publishedRecord.setArchivalState(ArchivalState.PUBLISHED);
      publishedRecord.setPid(HandleGenerator.generate());
      publishedRecord.setExternalId("foobarxyz");
      publishedRecord.setPublicationTimeStamp(Instant.now().minus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.MINUTES));

      DigitalObject linkedObject = new DigitalObject();
      linkedObject.setId(testDataSet.digitalObject().getId());
      publishedRecord.setDigitalObject(linkedObject);

      archivalRecordRepository.save(publishedRecord);

      var publishedRecords = archivalRecordService.findArchivalRecordsForObject(
          testDataSet.digitalObject().getId(),
          TEST_ARCHIVAL_STATES,
          Pageable.unpaged()
      );

      Assertions.assertThat(publishedRecords.getContent())
          .hasSize(1);

    }

    @Test
    void findsExpectedPublishedAndReservedArchivalRecords(){

      // use later to search only for both states
      final Set<ArchivalState> TEST_ARCHIVAL_STATES = Set.of(ArchivalState.RESERVED, ArchivalState.PUBLISHED);

      // add another published archival record
      ArchivalRecord publishedRecord = new ArchivalRecord();
      publishedRecord.setArchivalState(ArchivalState.PUBLISHED);
      publishedRecord.setPid(HandleGenerator.generate());
      publishedRecord.setExternalId("foobarxyz");
      publishedRecord.setPublicationTimeStamp(Instant.now().minus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.MINUTES));

      DigitalObject linkedObject = new DigitalObject();
      linkedObject.setId(testDataSet.digitalObject().getId());
      publishedRecord.setDigitalObject(linkedObject);

      archivalRecordRepository.save(publishedRecord);

      var publishedRecords = archivalRecordService.findArchivalRecordsForObject(
          testDataSet.digitalObject().getId(),
          TEST_ARCHIVAL_STATES,
          Pageable.unpaged()
      );

      Assertions.assertThat(publishedRecords.getContent())
          .hasSize(2);

    }

    @Test
    void findsExpectedArchivalRecords(){
      // use later to search only for both states
      final Set<ArchivalState> TEST_ARCHIVAL_STATES = Set.of();

      // add another published archival record
      ArchivalRecord publishedRecord = new ArchivalRecord();
      publishedRecord.setArchivalState(ArchivalState.PUBLISHED);
      publishedRecord.setPid(HandleGenerator.generate());
      publishedRecord.setExternalId("foobarxyz");
      publishedRecord.setPublicationTimeStamp(Instant.now().minus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.MINUTES));

      DigitalObject linkedObject = new DigitalObject();
      linkedObject.setId(testDataSet.digitalObject().getId());
      publishedRecord.setDigitalObject(linkedObject);

      archivalRecordRepository.save(publishedRecord);

      var publishedRecords = archivalRecordService.findArchivalRecordsForObject(
          testDataSet.digitalObject().getId(),
          TEST_ARCHIVAL_STATES,
          Pageable.unpaged()
      );

      Assertions.assertThat(publishedRecords.getContent())
          .hasSize(0);
    }

  }

  @Nested
  class FindActiveArchivalRecordForObject {

    @Test
    void successfullyFindsExpectedActiveArchivalRecord(){

      var foundRecord = archivalRecordService.findActiveArchivalRecordForObject(testDataSet.digitalObject().getId());

      Assertions.assertThat(foundRecord.getContent().getFirst().getPid())
          .isEqualTo(testDataSet.archivalRecord().getPid());

    }

    @Test
    void throwsIfDigitalObjectDoesNotExist(){

      final String NOT_EXISTENT_OBJECT_ID = testDataSet.project().getProjectAbbr() + ".foobar";
      Assertions.assertThatThrownBy(() -> archivalRecordService.findActiveArchivalRecordForObject(NOT_EXISTENT_OBJECT_ID))
          .isInstanceOf(DigitalObjectNotFoundException.class);

    }

    @Test
    void throwsIfNotActiveArchivalRecordExists(){

      // make sure that no archival records exist at all
      archivalRecordRepository.deleteAll();

      Assertions.assertThatThrownBy(
          () -> archivalRecordService.findActiveArchivalRecordForObject(testDataSet.digitalObject().getId()))
          .isInstanceOf(ArchivalRecordNoActiveRecordException.class);

    }

    @Test
    void throwsIfUnexpectedlyMultipleActiveRecordsExist(){

      ArchivalRecord another = new ArchivalRecord();
      another.setPid(HandleGenerator.generate());
      another.setArchivalState(ArchivalState.RESERVED);

      var linkedObject = new DigitalObject();
      linkedObject.setId(testDataSet.digitalObject().getId());
      another.setDigitalObject(linkedObject);

      archivalRecordRepository.save(another);

      Assertions.assertThatThrownBy(
              () -> archivalRecordService.findActiveArchivalRecordForObject(testDataSet.digitalObject().getId()))
          .isInstanceOf(ArchivalRecordInconsistentActiveRecordsException.class);

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
  class ReserveArchivalRecord {

    @Test
    void createsExpectedArchivalRecord(){

      var savedRecord = archivalRecordService.reserveArchivalRecord();

      Assertions.assertThat(
          archivalRecordRepository.existsById(savedRecord.getPid())
      ).isTrue();

    }

  }

  @Nested
  class ReserveArchivalRecordByPid {

    @Test
    void createsExpectedArchivalRecord(){

      final String TEST_PID = HandleGenerator.generate();

      var savedRecord = archivalRecordService.reserveArchivalRecordByPid(TEST_PID);

      Assertions.assertThat(
          archivalRecordRepository.existsById(savedRecord.getPid()))
          .isTrue();

    }

    @Test
    void throwsIfPidAlreadyExists(){

      final String ALREADY_EXISTING_PID = testDataSet.archivalRecord().getPid();

      Assertions.assertThatThrownBy(
          () -> archivalRecordService.reserveArchivalRecordByPid(ALREADY_EXISTING_PID)
      ).isInstanceOf(ArchivalRecordAlreadyExistsException.class);

    }

  }

  @Nested
  class ReserveArchivalRecordByObjectId {

    @Test
    void createsExpectedArchivalRecord(){

      // make sure that test archival record is deleted (otherwise clash)
      archivalRecordRepository.deleteAll();

      var savedArchivalRecord = archivalRecordService.reserveArchivalRecordByObjectId(testDataSet.digitalObject().getId());
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

      archivalRecordService.reserveArchivalRecordByObjectId(testDataSet.digitalObject().getId());

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
        archivalRecordService.reserveArchivalRecordByObjectId(testDataSet.digitalObject().getId());
      }).isInstanceOf(
          ArchivalRecordAlreadyActiveException.class
      );

    }

    @Test
    void throwsIfDigitalObjectWasNotFound(){

      final String NON_EXISTENT_OBJECT_ID = testDataSet.project().getProjectAbbr() + ".foobar";

      Assertions.assertThatThrownBy(() -> archivalRecordService.reserveArchivalRecordByObjectId(NON_EXISTENT_OBJECT_ID))
          .isInstanceOf(DigitalObjectNotFoundException.class);

    }


  }

  @Nested
  class ReserveArchivalRecordByObjectIdAndPid {

    @Test
    void createsExpectedArchivalRecordViaProvidingExternalPid(){

      final String TEST_PID = HandleGenerator.generate();

      var savedArchivalRecord = archivalRecordService.reserveArchivalRecordByObjectIdAndPid(testDataSet.digitalObject().getId(), TEST_PID);
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

      Assertions.assertThatThrownBy(() -> archivalRecordService.reserveArchivalRecordByObjectIdAndPid(NON_EXISTENT_OBJECT_ID, TEST_PID))
          .isInstanceOf(DigitalObjectNotFoundException.class);

    }

    @Test
    void throwsIfPidAlreadyExists(){

      final String ALREADY_EXISTING_PID = testDataSet.archivalRecord().getPid();
      Assertions.assertThatThrownBy(() -> archivalRecordService.reserveArchivalRecordByObjectIdAndPid(testDataSet.digitalObject().getId(), ALREADY_EXISTING_PID))
          .isInstanceOf(ArchivalRecordAlreadyExistsException.class);
    }

  }

  @Nested
  class UpdateArchivalRecord {

    @Test
    void changesExpectedArchivalRecord(){

      final String TEST_EXTERNAL_REFERENCE = "foobarxyz";
      final Instant TEST_TIMESTAMP = Instant.now();

      var changeDto = new ArchivalRecordDto();
      changeDto.setPid(testDataSet.archivalRecord().getPid());
      changeDto.setExternalId(TEST_EXTERNAL_REFERENCE);
      changeDto.setPublicationTimeStamp(TEST_TIMESTAMP);
      changeDto.setArchivalState(ArchivalState.PUBLISHED);

      var savedRecord = archivalRecordService.updateArchivalRecord(changeDto);

      Assertions.assertThat(savedRecord.getExternalId()).isEqualTo(TEST_EXTERNAL_REFERENCE);
      Assertions.assertThat(savedRecord.getPublicationTimeStamp()).isEqualTo(TEST_TIMESTAMP);
      Assertions.assertThat(savedRecord.getArchivalState()).isEqualTo(ArchivalState.PUBLISHED);

    }

    @Test
    void throwsIfArchivalRecordDoesNotExist(){

      final String TEST_PID = HandleGenerator.generate();

      var changeDto = new ArchivalRecordDto();
      changeDto.setPid(TEST_PID);
      changeDto.setExternalId("foobarxyz");
      changeDto.setPublicationTimeStamp(Instant.now());

      Assertions.assertThatThrownBy(() -> archivalRecordService.updateArchivalRecord(changeDto))
          .isInstanceOf(ArchivalRecordNotFoundException.class);

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
