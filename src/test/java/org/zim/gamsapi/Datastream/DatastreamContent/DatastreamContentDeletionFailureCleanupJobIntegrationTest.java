package org.zim.gamsapi.Datastream.DatastreamContent;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.auditing.AuditingHandler;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.zim.gamsapi.Datastream.utils.interfaces.IDatastreamContentRepository;
import org.zim.gamsapi.IntegrationTest;
import org.zim.gamsapi.TestUtilities.TestDataBuilder;
import org.zim.gamsapi.TestUtilities.TestDataSet;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class DatastreamContentDeletionFailureCleanupJobIntegrationTest extends IntegrationTest {

  @Autowired
  private DatastreamContentDeletionFailureCleanupJob cleanupJob;

  @Autowired
  private DatastreamContentDeletionFailureRepository failureRepository;

  @Autowired
  private IDatastreamContentRepository datastreamContentRepository;

  @MockitoBean
  private AuditingHandler auditingHandler;

  @Autowired
  TestDataBuilder testDataBuilder;

  TestDataSet testDataSet;

  @BeforeEach
  public void setup(){
    testDataSet = testDataBuilder.buildTestDataSet();
    Assertions.assertThat(
        datastreamContentRepository.exists(testDataSet.mainDatastream().deriveDatastreamId())
    ).isTrue();
  }

  @Test
  @DisplayName("Should successfully delete real file and remove failure record")
  public void shouldDeleteRealFileAndRemoveFailureRecord() {
    // Create a failed deletion record (for the datastream in  the test dataset)
    DatastreamContentDeletionFailure failure = DatastreamContentDeletionFailure.builder()
        .digitalObjectId(testDataSet.digitalObject().getId())
        .datastreamDsid(testDataSet.mainDatastream().getDsid())
        .retryCount(0)
        .build();
    // Save the failure record
    failureRepository.save(failure);

    // Run the cleanup job directly
    cleanupJob.processFailedDeletions();

    // Datastream content should be deleted
    Assertions.assertThat(
        datastreamContentRepository.exists(testDataSet.mainDatastream().deriveDatastreamId())
    ).isFalse();
    // failure record should be removed
    Assertions.assertThat(failureRepository.findAll()).isEmpty();
  }

  @Test
  public void doesNotRemoveTestDatastreamContentWhenNoFailureReportsAreAvailable() {

    // Run the cleanup job directly
    cleanupJob.processFailedDeletions();

    // The failure record should still exist as the file does not exist
    Assertions.assertThat(failureRepository.findAll()).isEmpty();
    Assertions.assertThat(datastreamContentRepository.exists(
        testDataSet.mainDatastream().deriveDatastreamId()
    )).isTrue();
  }


}
