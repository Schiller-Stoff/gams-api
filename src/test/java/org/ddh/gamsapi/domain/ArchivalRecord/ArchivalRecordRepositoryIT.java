package org.ddh.gamsapi.domain.ArchivalRecord;

import org.assertj.core.api.Assertions;
import org.ddh.gamsapi.IntegrationTest;
import org.ddh.gamsapi.TestUtilities.TestDataBuilder;
import org.ddh.gamsapi.TestUtilities.TestDataSet;
import org.ddh.gamsapi.domain.DigitalObject.DigitalObject;
import org.ddh.gamsapi.domain.DigitalObject.utils.interfaces.IDigitalObjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.auditing.AuditingHandler;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ArchivalRecordRepositoryIT extends IntegrationTest {

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
  class DetachAllFromDigitalObject {

    @Test
    void nullsDigitalObjectReferenceOnMatchingRecords() {
      archivalRecordRepository.detachAllFromDigitalObject(testDataSet.digitalObject().getId());

      ArchivalRecord persisted = archivalRecordRepository
          .findById(testDataSet.archivalRecord().getPid())
          .orElseThrow();

      Assertions.assertThat(persisted.getDigitalObject()).isNull();
    }

    @Test
    void doesNotDeleteTheArchivalRecordItself() {
      archivalRecordRepository.detachAllFromDigitalObject(testDataSet.digitalObject().getId());

      Assertions.assertThat(
          archivalRecordRepository.findById(testDataSet.archivalRecord().getPid())
      ).isPresent();
    }

    @Test
    void preservesOtherFieldsOnTheDetachedRecord() {
      String expectedExternalId = testDataSet.archivalRecord().getExternalId();

      archivalRecordRepository.detachAllFromDigitalObject(testDataSet.digitalObject().getId());

      ArchivalRecord persisted = archivalRecordRepository
          .findById(testDataSet.archivalRecord().getPid())
          .orElseThrow();

      Assertions.assertThat(persisted.getExternalId()).isEqualTo(expectedExternalId);
    }

    @Test
    void doesNotAffectArchivalRecordsOfOtherDigitalObjects() {
      DigitalObject otherObject = testDataBuilder.addRandomObject(testDataSet);

      ArchivalRecord otherRecord = new ArchivalRecord();
      otherRecord.setPid("other-object-record-" + System.currentTimeMillis());
      otherRecord.setDigitalObject(otherObject);
      otherRecord.setExternalId("some-external-id");
      archivalRecordRepository.save(otherRecord);

      archivalRecordRepository.detachAllFromDigitalObject(testDataSet.digitalObject().getId());

      ArchivalRecord persistedOther = archivalRecordRepository
          .findById(otherRecord.getPid())
          .orElseThrow();

      Assertions.assertThat(persistedOther.getDigitalObject()).isNotNull();
      Assertions.assertThat(persistedOther.getDigitalObject().getId()).isEqualTo(otherObject.getId());
    }

    @Test
    void isSafeToCallForADigitalObjectWithNoArchivalRecords() {
      DigitalObject objectWithoutRecords = testDataBuilder.addRandomProject(testDataSet) != null
          ? testDataBuilder.addRandomObject(testDataSet)
          : null;

      // detach for an id that isn't referenced by any archival record at all
      Assertions.assertThatCode(
          () -> archivalRecordRepository.detachAllFromDigitalObject("some-nonexistent-object-id")
      ).doesNotThrowAnyException();
    }

    /**
     * Regression test for the clearAutomatically = true setting: without it, a bulk JPQL update
     * bypasses the first-level cache, and an entity already loaded into the SAME persistence
     * context would keep showing its stale (non-null) digitalObject after the update.
     * @Transactional here is deliberate — it's what makes both repository calls share one
     * persistence context, which is the exact scenario this guards against.
     */
    @Test
    @Transactional
    void clearsPersistenceContextSoSubsequentReadsInTheSameTransactionSeeTheChange() {
      ArchivalRecord loadedBeforeUpdate = archivalRecordRepository
          .findById(testDataSet.archivalRecord().getPid())
          .orElseThrow();
      Assertions.assertThat(loadedBeforeUpdate.getDigitalObject()).isNotNull();

      archivalRecordRepository.detachAllFromDigitalObject(testDataSet.digitalObject().getId());

      ArchivalRecord loadedAfterUpdate = archivalRecordRepository
          .findById(testDataSet.archivalRecord().getPid())
          .orElseThrow();

      Assertions.assertThat(loadedAfterUpdate.getDigitalObject()).isNull();
    }
  }

}
