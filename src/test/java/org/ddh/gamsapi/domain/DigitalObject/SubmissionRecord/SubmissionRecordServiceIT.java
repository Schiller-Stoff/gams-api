package org.ddh.gamsapi.domain.DigitalObject.SubmissionRecord;


import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.auditing.AuditingHandler;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.ddh.gamsapi.IntegrationTest;
import org.ddh.gamsapi.TestUtilities.TestDataBuilder;
import org.ddh.gamsapi.TestUtilities.TestDataSet;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class SubmissionRecordServiceIT extends IntegrationTest {

  @Autowired
  ISubmissionRecordService submissionRecordService;

  // Deactivates the auditing process.
  @MockitoBean
  private AuditingHandler auditingHandler;

  @Autowired
  private TestDataBuilder testDataBuilder;

  @Nested
  public class Find {

    private TestDataSet testDataSet;

    @BeforeEach
    public void setup() {
      testDataSet = testDataBuilder.buildTestDataSet();
    }

    @Test
    public void foundSubmissionRecordIsNotNullAndHasNoNullFields() {
      var foundRecord = submissionRecordService.find(testDataSet.digitalObject().getId());
      Assertions.assertThat(foundRecord).isNotNull();
      Assertions.assertThat(foundRecord).hasNoNullFieldsOrProperties();
    }

    @Test
    public void findsExpectedSubmissionRecord() {
      var expectedRecord = submissionRecordService.find(testDataSet.digitalObject().getId());
      Assertions.assertThat(expectedRecord).isEqualTo(testDataSet.submissionRecord());
    }

  }

}
