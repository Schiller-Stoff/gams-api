package org.ddh.gamsapi.domain.ArchivalRecord;

import org.assertj.core.api.Assertions;
import org.ddh.gamsapi.IntegrationTest;
import org.ddh.gamsapi.TestUtilities.TestDataBuilder;
import org.ddh.gamsapi.TestUtilities.TestDataSet;
import org.ddh.gamsapi.domain.ArchivalRecord.utils.HandleGenerator;
import org.ddh.gamsapi.domain.DigitalObject.utils.exceptions.DigitalObjectNotFoundException;
import org.ddh.gamsapi.domain.DigitalObject.utils.interfaces.IDigitalObjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.auditing.AuditingHandler;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

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
      var savedArchivalRecord = archivalRecordService.createArchivalRecordByObjectId(testDataSet.digitalObject().getId());
      var foundArchivalRecord = archivalRecordRepository.findById(savedArchivalRecord.getPid()).orElseThrow();

      Assertions.assertThat(savedArchivalRecord)
          .isNotNull();

      Assertions.assertThat(savedArchivalRecord.getPid())
          .isEqualTo(foundArchivalRecord.getPid());


    }

    @Test
    void doesNotChangeLinkedDigitalObject(){

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
    void increasesAmountOfArchivalRecordsSaved(){

      archivalRecordService.createArchivalRecordByObjectId(testDataSet.digitalObject().getId());

      var foundRecords = archivalRecordRepository.findAllByDigitalObjectIdOrderByPublicationTimeStampDesc(testDataSet.digitalObject().getId());

      Assertions.assertThat(foundRecords).hasSize(2); // should now contain 2 (aside from test data)

    }

    @Test
    void throwsIfDigitalObjectWasNotFound(){

      final String NON_EXISTENT_OBJECT_ID = testDataSet.project().getProjectAbbr() + ".foobar";

      Assertions.assertThatThrownBy(() -> archivalRecordService.createArchivalRecordByObjectId(NON_EXISTENT_OBJECT_ID))
          .isInstanceOf(DigitalObjectNotFoundException.class);

    }


  }

  @Nested
  public class CreateArchivalRecordByObjectIdAndPid {

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


}
