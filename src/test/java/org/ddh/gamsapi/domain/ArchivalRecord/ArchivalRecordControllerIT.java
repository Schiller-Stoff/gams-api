package org.ddh.gamsapi.domain.ArchivalRecord;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.Assertions;
import org.ddh.gamsapi.IntegrationTest;
import org.ddh.gamsapi.TestUtilities.TestArchivalRecord;
import org.ddh.gamsapi.TestUtilities.TestDataBuilder;
import org.ddh.gamsapi.TestUtilities.TestDataSet;
import org.ddh.gamsapi.domain.ArchivalRecord.utils.ArchivalState;
import org.ddh.gamsapi.domain.ArchivalRecord.utils.HandleGenerator;
import org.ddh.gamsapi.domain.DigitalObject.DigitalObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.auditing.AuditingHandler;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;

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
  class DELETE {

    @Test
    void successfullyDeletesTestArchivalRecord() throws Exception {

      final String TEST_REQUEST_URL = String.format(
          "/api/curation/v1/archival-records/%s",
          testDataSet.archivalRecord().getPid()
      );

      mockMvc.perform(
          MockMvcRequestBuilders.delete(TEST_REQUEST_URL)
      ).andExpect(status().isOk());

      // test archival record should be deleted
      Assertions.assertThat(archivalRecordRepository.existsById(testDataSet.archivalRecord().getPid()))
          .isFalse();

    }

    @Test
    void throwsIfArchivalRecordWasNotFound() throws Exception {

      final String NON_EXISTENT_PID = TestArchivalRecord.PID.replace("1", "9");

      final String TEST_REQUEST_URL = String.format(
          "/api/curation/v1/archival-records/%s",
          NON_EXISTENT_PID
      );

      mockMvc.perform(
          MockMvcRequestBuilders.delete(TEST_REQUEST_URL)
      ).andExpect(status().isNotFound());

    }

    @Test
    void invalidPidWillCauseStatus400() throws Exception {

      final String INVALID_PID = "123456";
      final String TEST_REQUEST_URL = String.format(
          "/api/curation/v1/archival-records/%s",
          INVALID_PID
      );

      mockMvc.perform(
          MockMvcRequestBuilders.delete(TEST_REQUEST_URL)
      ).andExpect(status().isBadRequest());

    }

  }

  @Nested
  class GET {

    @Nested
    class FindArchivalRecords {

      @Test
      void responseContainsExpectedData() throws Exception {

        final String TEST_REQUEST_URL = String.format(
            "/api/curation/v1/archival-records?objectId=%s",
            testDataSet.digitalObject().getId()
        );

        String responseBody = mockMvc.perform(
            MockMvcRequestBuilders.get(TEST_REQUEST_URL)
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        Assertions.assertThat(responseBody)
            .isNotNull()
            .contains(testDataSet.archivalRecord().getPid())
            .contains(testDataSet.archivalRecord().getArchivalState().name())
            .contains(testDataSet.archivalRecord().getExternalId())

        ;

      }

      @Test
      void respondsWithExpectedFilteredArchivalRecords() throws Exception {

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

        final String TEST_REQUEST_URL = String.format(
            "/api/curation/v1/archival-records?objectId=%s&state=PUBLISHED",
            testDataSet.digitalObject().getId()
        );


        String responseBody = mockMvc.perform(
            MockMvcRequestBuilders.get(TEST_REQUEST_URL)
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        Assertions.assertThat(responseBody)
            .contains(publishedRecord.getPid())
            .doesNotContain(testDataSet.archivalRecord().getPid())
            .doesNotContain(ArchivalState.RESERVED.name());

      }

    }

    @Nested
    class FindActiveArchivalRecord {

      @Test
      void returnsExpectedActiveArchivalRecord() throws Exception {

        final String TEST_REQUEST_URL = String.format(
            "/api/curation/v1/archival-records/active?objectId=%s",
            testDataSet.digitalObject().getId()
        );

        String responseBody = mockMvc.perform(
            MockMvcRequestBuilders.get(TEST_REQUEST_URL)
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        Assertions.assertThat(responseBody)
            .contains(testDataSet.archivalRecord().getPid())
            .contains(testDataSet.archivalRecord().getArchivalState().name())
            .contains(testDataSet.archivalRecord().getExternalId());

      }

    }

    @Test
    void returnsErrorIfNoActiveRecordWasFound() throws Exception {

      // first delete all archival records
      archivalRecordRepository.deleteAll();

      final String TEST_REQUEST_URL = String.format(
          "/api/curation/v1/archival-records/active?objectId=%s",
          testDataSet.digitalObject().getId()
      );

      String responseBody = mockMvc.perform(
              MockMvcRequestBuilders.get(TEST_REQUEST_URL)
                  .contentType(MediaType.APPLICATION_JSON)
          ).andExpect(status().isBadRequest())
          .andReturn().getResponse().getContentAsString();

      Assertions.assertThat(responseBody)
          .contains("No active records found.");

    }

  }

  @Nested
  class POST {

    @Test
    void createsExpectedArchivalRecordWithObjectId() throws Exception {

      // delete existing archival records from test data
      archivalRecordRepository.deleteAll();

      final String REQUEST_URL = String.format(
          "/api/curation/v1/archival-records?objectId=%s",
          testDataSet.digitalObject().getId()
      );

      String response = mockMvc.perform(
              MockMvcRequestBuilders.post(REQUEST_URL)
          ).andExpect(status().isOk())
          .andReturn().getResponse().getContentAsString();

      var parsedResponse = new ObjectMapper().readValue(response, ArchivalRecord.class);
      String createdPid = parsedResponse.getPid();

      Assertions.assertThat(
          archivalRecordRepository.existsById(createdPid)
      ).isTrue();

    }


  }

  @Nested
  class PUT {

    @Nested
    class DraftArchivalRecord {

      @Test
      void draftsExpectedArchivalRecord() throws Exception {

        final String TEST_EXTERNAL_ID = "foobarxyz";

        final String REQUEST_URL = String.format(
            "/api/curation/v1/archival-records/draft/%s",
            testDataSet.archivalRecord().getPid()
        );

        final String BODY = String.format(
            "{\"externalId\":\"%s\"}",
            TEST_EXTERNAL_ID
        );

        String response = mockMvc.perform(
            MockMvcRequestBuilders.put(REQUEST_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY)
        ).andExpect(status().isOk()
        ).andReturn().getResponse().getContentAsString();

        Assertions.assertThat(response)
            .contains(TEST_EXTERNAL_ID)
            .contains(ArchivalState.DRAFT.name())
            .contains("null") // publicationDate should be null
            .contains(testDataSet.archivalRecord().getPid());

      }

      @Test
      void throwsIfExternalIdIsTooShort() throws Exception {

        final String TEST_EXTERNAL_ID = "fo";

        final String REQUEST_URL = String.format(
            "/api/curation/v1/archival-records/draft/%s",
            testDataSet.archivalRecord().getPid()
        );

        final String BODY = String.format(
            "{\"externalId\":\"%s\"}",
            TEST_EXTERNAL_ID
        );

        mockMvc.perform(
            MockMvcRequestBuilders.put(REQUEST_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY)
        ).andExpect(status().isBadRequest());

      }

      @Test
      void throwsIfNoReservedRecordIsAvailable() throws Exception {

        final String TEST_EXTERNAL_ID = "foobarxyz";

        // first set test record to DRAFT state.
        var archivalRecord = archivalRecordRepository.findById(testDataSet.archivalRecord().getPid())
            .orElseThrow();
        archivalRecord.setArchivalState(ArchivalState.DRAFT);
        archivalRecord.setExternalId(TEST_EXTERNAL_ID);
        archivalRecordRepository.save(archivalRecord);

        final String REQUEST_URL = String.format(
            "/api/curation/v1/archival-records/draft/%s",
            testDataSet.archivalRecord().getPid()
        );

        final String BODY = String.format(
            "{\"externalId\":\"%s\"}",
            TEST_EXTERNAL_ID
        );

        mockMvc.perform(
            MockMvcRequestBuilders.put(REQUEST_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY)
        ).andExpect(status().isBadRequest());

      }


      @Test
      void throwsIfOnlyPublishedRecordIsAvailable() throws Exception {

        final String TEST_EXTERNAL_ID = "foobarxyz";

        // first set test record to PUBLISHED state.
        var archivalRecord = archivalRecordRepository.findById(testDataSet.archivalRecord().getPid())
            .orElseThrow();
        archivalRecord.setArchivalState(ArchivalState.PUBLISHED);
        archivalRecord.setExternalId(TEST_EXTERNAL_ID);
        archivalRecordRepository.save(archivalRecord);

        final String REQUEST_URL = String.format(
            "/api/curation/v1/archival-records/draft/%s",
            testDataSet.archivalRecord().getPid()
        );

        final String BODY = String.format(
            "{\"externalId\":\"%s\"}",
            TEST_EXTERNAL_ID
        );

        mockMvc.perform(
            MockMvcRequestBuilders.put(REQUEST_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY)
        ).andExpect(status().isBadRequest());

      }

    }

    @Nested
    class PublishArchivalRecord {

      @Test
      void publishesExpectedArchivalRecord() throws Exception {

        // first bring test archival record to drafted state
        final String TEST_EXTERNAL_ID = "foobarxyz";
        final Instant TEST_PUBLICATION_DATE = Instant.now().truncatedTo(ChronoUnit.SECONDS);

        var testArchivalRecord = archivalRecordRepository.findById(testDataSet.archivalRecord().getPid())
            .orElseThrow();

        testArchivalRecord.setArchivalState(ArchivalState.DRAFT);
        testArchivalRecord.setExternalId(TEST_EXTERNAL_ID);
        archivalRecordRepository.save(testArchivalRecord);

        final String REQUEST_URL = String.format(
            "/api/curation/v1/archival-records/published/%s",
            testDataSet.archivalRecord().getPid()
        );

        final String BODY = String.format(
            "{\"publicationTimeStamp\":\"%s\"}",
            TEST_PUBLICATION_DATE
        );

        String response = mockMvc.perform(
            MockMvcRequestBuilders.put(REQUEST_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY)
        ).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        Assertions.assertThat(response)
            .contains(ArchivalState.PUBLISHED.name())
            .contains(TEST_PUBLICATION_DATE.toString())
            .contains(TEST_EXTERNAL_ID)
            .contains(testArchivalRecord.getPid());

      }

      @Test
      void returnsErrorIfGivenPublicationTimestampIsInvalid() throws Exception {

        final String INVALID_DATE_FORMAT = "this is no date";

        final String REQUEST_URL = String.format(
            "/api/curation/v1/archival-records/published/%s",
            testDataSet.archivalRecord().getPid()
        );

        final String BODY = String.format(
            "{\"publicationTimeStamp\":\"%s\"}",
            INVALID_DATE_FORMAT
        );

        mockMvc.perform(
            MockMvcRequestBuilders.put(REQUEST_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY)
        ).andExpect(status().isBadRequest());

      }

      @Test
      void errorIfPublicationsTimestampIsInTheFuture() throws Exception {

        final String INVALID_DATE_FORMAT = Instant.now().plus(1, ChronoUnit.MINUTES).toString();

        final String REQUEST_URL = String.format(
            "/api/curation/v1/archival-records/published/%s",
            testDataSet.archivalRecord().getPid()
        );

        final String BODY = String.format(
            "{\"publicationTimeStamp\":\"%s\"}",
            INVALID_DATE_FORMAT
        );

        String response = mockMvc.perform(
            MockMvcRequestBuilders.put(REQUEST_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY)
        ).andExpect(status().isBadRequest())
            .andReturn().getResponse().getContentAsString();

        Assertions.assertThat(response.toLowerCase())
            .contains("validation failed")
            .contains("publicationtimestamp");

      }


    }

    @Test
    void createsExpectedArchivalRecord() throws Exception {

      final String TEST_PID = HandleGenerator.generate();

      final String REQUEST_URL = String.format(
          "/api/curation/v1/archival-records/%s?objectId=%s",
          TEST_PID,
          testDataSet.digitalObject().getId()
      );

      mockMvc.perform(
              MockMvcRequestBuilders.put(REQUEST_URL)
          ).andExpect(status().isOk()
      );

      Assertions.assertThat(
          archivalRecordRepository.existsById(TEST_PID)
      ).isTrue();

    }

    @Test
    void createsExpectedArchivalRecordWithObjectId() throws Exception {

      final String TEST_PID = HandleGenerator.generate();

      final String REQUEST_URL = String.format(
          "/api/curation/v1/archival-records/%s?objectId=%s",
          TEST_PID,
          testDataSet.digitalObject().getId()
      );

      mockMvc.perform(
              MockMvcRequestBuilders.put(REQUEST_URL)
          ).andExpect(status().isOk());

      Assertions.assertThat(
          archivalRecordRepository.existsById(TEST_PID)
      ).isTrue();

      Assertions.assertThat(
          archivalRecordRepository.findAllByDigitalObjectIdOrderByPublicationTimeStampDesc(
              testDataSet.digitalObject().getId(),
              Pageable.unpaged()
          )
      ).hasSize(2); // has size 2 now!

    }

  }

  @Nested
  class PATCH {

    @Test
    void successfullyPatchesGivenArchivalRecord() throws Exception {

      final Instant TEST_PUBLICATION_TIMESTAMP = Instant.now();
      final String TEST_EXTERNAL_ID = "foobarxyz";

      final String REQUEST_URL = String.format(
          "/api/curation/v1/archival-records/%s",
          testDataSet.archivalRecord().getPid()
      );

      final String body = String.format(
          "{\"publicationTimeStamp\": \"%s\",\"externalId\":\"%s\", \"archivalState\":\"PUBLISHED\"}",
          TEST_PUBLICATION_TIMESTAMP,
          TEST_EXTERNAL_ID
      );

      String response = mockMvc.perform(
          MockMvcRequestBuilders.patch(REQUEST_URL)
              .contentType(MediaType.APPLICATION_JSON)
              .content(body)
        ).andExpect(status().isOk()
      ).andReturn().getResponse().getContentAsString();

      Assertions.assertThat(response)
          .contains(TEST_EXTERNAL_ID)
          .contains(TEST_PUBLICATION_TIMESTAMP.toString())
          .contains(testDataSet.archivalRecord().getPid())
          .contains(ArchivalState.PUBLISHED.name())
      ;

    }

    @Test
    void throwsIfGivenPidWasInvalid() throws Exception {

      final String INVALID_PID = "foobar_bar";

      final String REQUEST_URL = String.format(
          "/api/curation/v1/archival-records/%s",
          INVALID_PID
      );

      final String body = String.format(
          "{\"publicationTimeStamp\": \"%s\",\"externalId\":\"%s\", \"archivalState\":\"PUBLISHED\"}",
          Instant.now(),
          "foobarxyz"
      );

      mockMvc.perform(
          MockMvcRequestBuilders.patch(REQUEST_URL)
              .contentType(MediaType.APPLICATION_JSON)
              .content(body)
      ).andExpect(status().isBadRequest());

    }

    @Test
    void throwsIfArchivalRecordDoesntExist() throws Exception {

      final Instant TEST_PUBLICATION_TIMESTAMP = Instant.now();
      final String TEST_EXTERNAL_ID = "foobarxyz";

      final String DIFFERENT_PID = testDataSet.archivalRecord().getPid().replace("8","2");

      final String REQUEST_URL = String.format(
          "/api/curation/v1/archival-records/%s",
          DIFFERENT_PID
      );

      final String body = String.format(
          "{\"publicationTimeStamp\": \"%s\",\"externalId\":\"%s\", \"archivalState\":\"PUBLISHED\"}",
          TEST_PUBLICATION_TIMESTAMP,
          TEST_EXTERNAL_ID
      );

      mockMvc.perform(
          MockMvcRequestBuilders.patch(REQUEST_URL)
              .contentType(MediaType.APPLICATION_JSON)
              .content(body)
      ).andExpect(status().isNotFound());

    }

  }

}
