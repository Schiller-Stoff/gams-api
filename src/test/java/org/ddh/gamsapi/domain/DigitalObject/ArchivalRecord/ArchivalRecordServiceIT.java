package org.ddh.gamsapi.domain.DigitalObject.ArchivalRecord;

import org.assertj.core.api.Assertions;
import org.ddh.gamsapi.IntegrationTest;
import org.ddh.gamsapi.TestUtilities.TestArchivalRecord;
import org.ddh.gamsapi.TestUtilities.TestDataBuilder;
import org.ddh.gamsapi.TestUtilities.TestDataSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.auditing.AuditingHandler;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ArchivalRecordServiceIT extends IntegrationTest {

  @Autowired
  IArchivalRecordService archivalRecordService;

  // Deactivates the auditing process.
  @MockitoBean
  private AuditingHandler auditingHandler;

  @Autowired
  private TestDataBuilder testDataBuilder;

  private TestDataSet testDataSet;

  @BeforeEach
  public void setup() {
    testDataSet = testDataBuilder.buildTestDataSet();
  }

  @Nested
  public class FindForObject {

    @Test
    public void foundArchivalRecordIsNotNullAndIsNotEmpty(){
      var foundRecord = archivalRecordService.findForObject(testDataSet.digitalObject().getId());
      Assertions.assertThat(foundRecord).isNotNull();
      Assertions.assertThat(foundRecord).isNotEmpty();
      Assertions.assertThat(foundRecord).hasSize(1);
    }

    @Test
    public void findsExpectedArchivalRecord(){
      var expectedRecords = archivalRecordService.findForObject(testDataSet.digitalObject().getId());
      Assertions.assertThat(
          expectedRecords.getFirst().getId()
      ).isEqualTo(testDataSet.archivalRecord().getId());
    }

  }


  @Nested
  public class Save {

    @Test
    public void savesExpectedAdditionalArchivalRecord(){

      ArchivalRecordCreateDto archivalRecordCreateDto = new ArchivalRecordCreateDto();
      archivalRecordCreateDto.setDigitalObjectId(testDataSet.digitalObject().getId());
      archivalRecordCreateDto.setPid(testDataSet.archivalRecord().getPid());
      archivalRecordCreateDto.setTimeStamp(Instant.now());
      archivalRecordService.save(archivalRecordCreateDto);

      var foundRecords = archivalRecordService.findForObject(testDataSet.digitalObject().getId());

      Assertions.assertThat(foundRecords).hasSize(2);

    }

  }

}
