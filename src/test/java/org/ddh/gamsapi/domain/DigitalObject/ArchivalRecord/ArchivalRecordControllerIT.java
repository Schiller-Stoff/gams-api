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
class ArchivalRecordControllerIT extends IntegrationTest {

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
  void setup() {
    testDataSet = testDataBuilder.buildTestDataSet();
  }

  @Nested
  class GET {

    @Nested
    class JSONResponse {

      @Test
      void jsonContainsExpectedPid() throws Exception {

        final String TEST_REQUEST_URL = String.format(
            "/api/curation/v1/projects/%s/objects/%s/archival-records",
            testDataSet.project().getProjectAbbr(),
            testDataSet.digitalObject().getId()
        );

        String responseBody = mockMvc.perform(
            MockMvcRequestBuilders.get(TEST_REQUEST_URL)
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        Assertions.assertThat(responseBody)
            .isNotNull()
            .contains(
                testDataSet.archivalRecord().getPid()
            );

      }

    }

  }

  @Nested
  class POST {

    @Test
    void failsToCreateAnArchivalRecordWithoutExternalId() throws Exception {

      final String TEST_REQUEST_URL = String.format(
          "/api/curation/v1/projects/%s/objects/%s/archival-records",
          testDataSet.project().getProjectAbbr(),
          testDataSet.digitalObject().getId()
      );

      final String TEST_REQUEST_BODY = String.format(
          "{\"pid\":\"%s\",\"timeStamp\":\"%s\",\"archivingStatus\":\"%s\"}",
          testDataSet.archivalRecord().getPid(),
          testDataSet.archivalRecord().getTimeStamp(),
          testDataSet.archivalRecord().getArchivingStatus()
      );

      mockMvc.perform(
              MockMvcRequestBuilders.post(TEST_REQUEST_URL)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(TEST_REQUEST_BODY)
          ).andExpect(status().is4xxClientError());

      var foundRecords = archivalRecordRepository.findAllByDigitalObjectIdOrderByTimeStampDesc(
          testDataSet.digitalObject().getId()
      );

      // now an additional archival record should NOT exist (next to the one in the test data set)
      Assertions.assertThat(foundRecords).hasSize(1);

    }

    @Test
    void failsToCreateAnArchivalRecordWithoutArchivingStatus() throws Exception {

      final String TEST_REQUEST_URL = String.format(
          "/api/curation/v1/projects/%s/objects/%s/archival-records",
          testDataSet.project().getProjectAbbr(),
          testDataSet.digitalObject().getId()
      );

      final String TEST_REQUEST_BODY = String.format(
          "{\"pid\":\"%s\",\"timeStamp\":\"%s\",\"externalId\":\"%s\"}",
          testDataSet.archivalRecord().getPid(),
          testDataSet.archivalRecord().getTimeStamp(),
          testDataSet.archivalRecord().getExternalId()
      );

      mockMvc.perform(
          MockMvcRequestBuilders.post(TEST_REQUEST_URL)
              .contentType(MediaType.APPLICATION_JSON)
              .content(TEST_REQUEST_BODY)
      ).andExpect(status().is4xxClientError());

      var foundRecords = archivalRecordRepository.findAllByDigitalObjectIdOrderByTimeStampDesc(
          testDataSet.digitalObject().getId()
      );

      // now an additional archival record should NOT exist (next to the one in the test data set)
      Assertions.assertThat(foundRecords).hasSize(1);

    }

    @Test
    void successfullyCreatesAnArchivalRecord() throws Exception {

      final String TEST_REQUEST_URL = String.format(
          "/api/curation/v1/projects/%s/objects/%s/archival-records",
          testDataSet.project().getProjectAbbr(),
          testDataSet.digitalObject().getId()
      );

      final String TEST_REQUEST_BODY = String.format(
          "{\"pid\":\"%s\",\"timeStamp\":\"%s\",\"externalId\":\"%s\",\"archivingStatus\":\"DRAFTED\"}",
          testDataSet.archivalRecord().getPid(),
          testDataSet.archivalRecord().getTimeStamp(),
          testDataSet.archivalRecord().getExternalId()
      );

      mockMvc.perform(
          MockMvcRequestBuilders.post(TEST_REQUEST_URL)
              .contentType(MediaType.APPLICATION_JSON)
              .content(TEST_REQUEST_BODY)
      ).andExpect(status().isOk());

      var foundRecords = archivalRecordRepository.findAllByDigitalObjectIdOrderByTimeStampDesc(
          testDataSet.digitalObject().getId()
      );

      // now an additional archival record should exist (next to the one in the test data set)
      Assertions.assertThat(foundRecords).hasSize(2);

    }

  }

}
