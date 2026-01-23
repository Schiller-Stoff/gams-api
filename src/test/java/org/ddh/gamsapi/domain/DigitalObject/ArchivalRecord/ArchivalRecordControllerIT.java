package org.ddh.gamsapi.domain.DigitalObject.ArchivalRecord;

import org.assertj.core.api.Assertions;
import org.ddh.gamsapi.IntegrationTest;
import org.ddh.gamsapi.TestUtilities.TestDataBuilder;
import org.ddh.gamsapi.TestUtilities.TestDataSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.auditing.AuditingHandler;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc(addFilters = false) // deactivates spring security for the test class
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ArchivalRecordControllerIT extends IntegrationTest {

  @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
  @Autowired
  private MockMvc mockMvc;

  // deactivate auditing process
  @MockitoBean
  private AuditingHandler auditingHandler;

  private TestDataSet testDataSet;

  @Autowired
  private TestDataBuilder testDataBuilder;

  @Autowired
  private IArchivalRecordRepository archivalRecordRepository;

  @BeforeEach
  public void setup() {
    testDataSet = testDataBuilder.buildTestDataSet();
  }

  @Nested
  public class POST {

    @Test
    public void createsAnAdditionalArchivalRecord() throws Exception {

      final String TEST_REQUEST_URL = String.format(
          "/api/v1/projects/%s/objects/%s/archival-records",
          testDataSet.project().getProjectAbbr(),
          testDataSet.digitalObject().getId()
      );

      final String TEST_REQUEST_BODY = String.format(
          "{\"pid\":\"%s\",\"timeStamp\":\"%s\"}",
          testDataSet.archivalRecord().getPid(),
          testDataSet.archivalRecord().getTimeStamp()
      );

      mockMvc.perform(
              MockMvcRequestBuilders.post(TEST_REQUEST_URL)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(TEST_REQUEST_BODY)
          ).andExpect(status().isOk());

      // perform test

      var foundRecords = archivalRecordRepository.findAllByDigitalObjectIdOrderByTimeStampDesc(testDataSet.digitalObject().getId());

      // now an additional archival record should exist (next to the one in the test data set)
      Assertions.assertThat(foundRecords).hasSize(2);




    }

  }

}
