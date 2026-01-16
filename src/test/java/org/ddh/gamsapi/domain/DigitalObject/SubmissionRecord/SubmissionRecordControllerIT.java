package org.ddh.gamsapi.domain.DigitalObject.SubmissionRecord;


import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.data.auditing.AuditingHandler;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.ddh.gamsapi.IntegrationTest;
import org.ddh.gamsapi.TestUtilities.TestDataBuilder;
import org.ddh.gamsapi.TestUtilities.TestDataSet;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc(addFilters = false) // deactivates spring security for the test class
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class SubmissionRecordControllerIT extends IntegrationTest {

  @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
  @Autowired
  private MockMvc mockMvc;

  // deactivate auditing process
  @MockitoBean
  private AuditingHandler auditingHandler;

  private TestDataSet testDataSet;

  @Autowired
  private TestDataBuilder testDataBuilder;

  @BeforeEach
  public void setup() {
    testDataSet = testDataBuilder.buildTestDataSet();
  }

  @Nested
  public class GET {

    @Test
    public void returnsExpectedSubmissionRecord() throws Exception {

      String REQUEST_URL = String.format(
          "/api/v1/projects/%s/objects/%s/record",
          testDataSet.project().getProjectAbbr(),
          testDataSet.digitalObject().getId()
      );

      // Act
      MvcResult mvcResult = mockMvc.perform(
              MockMvcRequestBuilders.get(REQUEST_URL)
          )
          .andExpect(status().isOk())
          .andExpect(result -> result
              .getResponse()
              .getContentType()
              .equals(MediaType.APPLICATION_JSON_VALUE))
          .andReturn();

      // Assert
      String response = mvcResult.getResponse().getContentAsString();

      System.out.println("Response: " + response);

      Assertions.assertThat(response)
          .contains(
              testDataSet.submissionRecord().getId(),
              testDataSet.submissionRecord().getBagSchema(),
              testDataSet.submissionRecord().getBagCreatedBy(),
              testDataSet.submissionRecord().getBagSource(),
              testDataSet.submissionRecord().getBaggingDate(),
              testDataSet.submissionRecord().getBagContactMail(),
              testDataSet.submissionRecord().getBagExternalDescription(),
              testDataSet.submissionRecord().getBagPayloadOxum(),
              testDataSet.submissionRecord().getBagVersion(),
              testDataSet.submissionRecord().getBagTagFileCharacterEncoding()
          );

    }

  }

}
