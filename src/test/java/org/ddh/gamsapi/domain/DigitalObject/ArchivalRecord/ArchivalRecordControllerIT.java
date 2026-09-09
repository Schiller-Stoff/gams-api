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
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.auditing.AuditingHandler;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

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

      @Test
      void getPublicArchivalRecordsDoesNotReturnPidOfTestArchivalRecord() throws Exception {

        final String TEST_REQUEST_URL = String.format(
            "/api/curation/v1/projects/%s/objects/%s/archival-records/public",
            testDataSet.project().getProjectAbbr(),
            testDataSet.digitalObject().getId()
        );

        String responseBody = mockMvc.perform(
            MockMvcRequestBuilders.get(TEST_REQUEST_URL)
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        // test-data archival record is drafted but endpoint should only return public ones
        Assertions.assertThat(testDataSet.archivalRecord().getArchivingStatus()).isEqualTo(ArchivingStatus.RESERVED);

        Assertions.assertThat(responseBody)
            .isNotNull()
            .doesNotContain(
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
          "{\"pid\":\"%s\",\"timeStamp\":\"%s\"}",
          testDataSet.archivalRecord().getPid(),
          testDataSet.archivalRecord().getTimeStamp()
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

      // make sure that nothing exists first
      archivalRecordRepository.deleteAll();

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
      ).andExpect(status().isOk());

      var foundRecords = archivalRecordRepository.findAllByDigitalObjectIdOrderByTimeStampDesc(
          testDataSet.digitalObject().getId()
      );

      // now an additional archival record should not exist - but still be one
      Assertions.assertThat(foundRecords).hasSize(1);

    }

    @Test
    void cannotCreatIfABlockingArchivalRecordAlreadyExists() throws Exception {

      // first delete available test record
      archivalRecordRepository.deleteAll();

      // create new
      var archivalRecord = TestArchivalRecord.generate(
          testDataSet.digitalObject(),
          "foo",
          "bar",
          ArchivingStatus.RESERVED
      );
      archivalRecordRepository.save(archivalRecord);


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
      ).andExpect(status().is(409));


    }


  }

  @Nested
  class PATCH_DRAFT {

    @Test
    void updatesArchivalRecordToExpectedValues() throws Exception {

      final String TEST_EXTERNAL_ID = "fooBar";
      final String TEST_PID = "10.5281/zenodo.22658867";
      final Instant TEST_TIME = Instant.now();

      final String TEST_REQUEST_URL = String.format(
          "/api/curation/v1/projects/%s/objects/%s/archival-records/draft",
          testDataSet.project().getProjectAbbr(),
          testDataSet.digitalObject().getId()
      );

      final String TEST_REQUEST_BODY = String.format(
          "{\"pid\":\"%s\",\"externalId\":\"%s\",\"timeStamp\":\"%s\"}",
          TEST_PID,
          TEST_EXTERNAL_ID,
          TEST_TIME
      );

      mockMvc.perform(
          MockMvcRequestBuilders.patch(TEST_REQUEST_URL)
              .contentType(MediaType.APPLICATION_JSON)
              .content(TEST_REQUEST_BODY)
      ).andExpect(status().isOk());

      var draftedRecord = archivalRecordRepository.findById(testDataSet.archivalRecord().getId()).orElseThrow();

      // archival record should now be in drafted state
      Assertions.assertThat(draftedRecord.getArchivingStatus()).isEqualTo(ArchivingStatus.DRAFTED);
      Assertions.assertThat(draftedRecord.getExternalId()).isEqualTo(TEST_EXTERNAL_ID);
      Assertions.assertThat(draftedRecord.getTimeStamp().truncatedTo(ChronoUnit.SECONDS)).isEqualTo(TEST_TIME.truncatedTo(ChronoUnit.SECONDS));
      Assertions.assertThat(draftedRecord.getPid()).isEqualTo(TEST_PID);

    }

    @Test
    void throwsIfDigitalObjectDoesNotExist() throws Exception {

      final String VALID_NON_EXISTENT_OBJECT_ID = testDataSet.project().getProjectAbbr() + ".foobar";

      final String TEST_REQUEST_URL = String.format(
          "/api/curation/v1/projects/%s/objects/%s/archival-records/draft",
          testDataSet.project().getProjectAbbr(),
          VALID_NON_EXISTENT_OBJECT_ID
      );

      final String TEST_REQUEST_BODY = String.format(
          "{\"pid\":\"%s\",\"externalId\":\"%s\",\"timeStamp\":\"%s\"}",
          testDataSet.archivalRecord().getPid(),
          testDataSet.archivalRecord().getExternalId(),
          testDataSet.archivalRecord().getTimeStamp()
      );

      mockMvc.perform(
          MockMvcRequestBuilders.patch(TEST_REQUEST_URL)
              .contentType(MediaType.APPLICATION_JSON)
              .content(TEST_REQUEST_BODY)
      ).andExpect(status().is(404));


    }

    @Test
    void throwsIfNoArchivalRecordsAreAvailable() throws Exception {

      // first make sure that no archival records exist
      archivalRecordRepository.deleteAll();

      final String TEST_EXTERNAL_ID = "fooBar";
      final String TEST_PID = "10.5281/zenodo.22658867";
      final Instant TEST_TIME = Instant.now();

      final String TEST_REQUEST_URL = String.format(
          "/api/curation/v1/projects/%s/objects/%s/archival-records/draft",
          testDataSet.project().getProjectAbbr(),
          testDataSet.digitalObject().getId()
      );

      final String TEST_REQUEST_BODY = String.format(
          "{\"pid\":\"%s\",\"externalId\":\"%s\",\"timeStamp\":\"%s\"}",
          TEST_PID,
          TEST_EXTERNAL_ID,
          TEST_TIME
      );

      mockMvc.perform(
          MockMvcRequestBuilders.patch(TEST_REQUEST_URL)
              .contentType(MediaType.APPLICATION_JSON)
              .content(TEST_REQUEST_BODY)
      ).andExpect(status().is(400));

    }

    @Test
    void throwsIfNoActiveArchivalRecordIsAvailable() throws Exception {

      // set status of test record to PUBLISHED -> so non draft can be found.
      var testRecord = archivalRecordRepository.findById(testDataSet.archivalRecord().getId()).orElseThrow();
      testRecord.setArchivingStatus(ArchivingStatus.PUBLISHED);
      archivalRecordRepository.save(testRecord);

      final String TEST_REQUEST_URL = String.format(
          "/api/curation/v1/projects/%s/objects/%s/archival-records/draft",
          testDataSet.project().getProjectAbbr(),
          testDataSet.digitalObject().getId()
      );

      final String TEST_REQUEST_BODY = String.format(
          "{\"pid\":\"%s\",\"externalId\":\"%s\",\"timeStamp\":\"%s\"}",
          testDataSet.archivalRecord().getPid(),
          testDataSet.archivalRecord().getExternalId(),
          testDataSet.archivalRecord().getTimeStamp()
      );

      mockMvc.perform(
          MockMvcRequestBuilders.patch(TEST_REQUEST_URL)
              .contentType(MediaType.APPLICATION_JSON)
              .content(TEST_REQUEST_BODY)
      ).andExpect(status().is(400));

    }


  }

  @Nested
  class PATCH_PUBLISH {

    @Test
    void throwsIfOnlyReservedArchivalRecordExist() throws Exception {

      // test data record is in reserved state -> should throw when trying to directly call publish.

      final String TEST_REQUEST_URL = String.format(
          "/api/curation/v1/projects/%s/objects/%s/archival-records/publish",
          testDataSet.project().getProjectAbbr(),
          testDataSet.digitalObject().getId()
      );

      final Instant TEST_TIME = Instant.now();
      final String TEST_REQUEST_BODY = String.format(
          "{\"timeStamp\":\"%s\"}",
          TEST_TIME
      );

      mockMvc.perform(
          MockMvcRequestBuilders.patch(TEST_REQUEST_URL)
              .contentType(MediaType.APPLICATION_JSON)
              .content(TEST_REQUEST_BODY)
      ).andExpect(status().is(400));


    }


    @Test
    void updatesArchivalRecordWithExpectedValues() throws Exception {

      // first delete test entry
      archivalRecordRepository.deleteAll();

      // create test record in DRAFT state
      final String TEST_EXTERNAL_ID = "fooBar";
      final String TEST_PID = "10.5281/zenodo.22658867";

      ArchivalRecord archivalRecord = new ArchivalRecord();
      archivalRecord.setExternalId(TEST_EXTERNAL_ID);
      archivalRecord.setTimeStamp(Instant.now()); // truncated to different value
      archivalRecord.setPid(TEST_PID);
      archivalRecord.setArchivingStatus(ArchivingStatus.DRAFTED); // at first in drafted state
      archivalRecord.setDigitalObject(testDataSet.digitalObject());
      var savedTestArchivalRecord = archivalRecordRepository.save(archivalRecord);

      //
      // test publish functionality
      //

      final String TEST_REQUEST_URL = String.format(
          "/api/curation/v1/projects/%s/objects/%s/archival-records/publish",
          testDataSet.project().getProjectAbbr(),
          testDataSet.digitalObject().getId()
      );

      final Instant TEST_TIME = Instant.now();
      final String TEST_REQUEST_BODY = String.format(
          "{\"timeStamp\":\"%s\"}",
          TEST_TIME
      );

      mockMvc.perform(
          MockMvcRequestBuilders.patch(TEST_REQUEST_URL)
              .contentType(MediaType.APPLICATION_JSON)
              .content(TEST_REQUEST_BODY)
      ).andExpect(status().isOk());


      // check if values are as expected
      var updatedTestArchivalRecordOptional =  archivalRecordRepository.findById(savedTestArchivalRecord.getId());

      Assertions.assertThat(updatedTestArchivalRecordOptional).isNotEmpty();

      var updatedTestArchivalRecord = updatedTestArchivalRecordOptional.get();

      Assertions.assertThat(updatedTestArchivalRecord.getId())
          .isEqualTo(savedTestArchivalRecord.getId());

      Assertions.assertThat(updatedTestArchivalRecord.getArchivingStatus())
          .isEqualTo(ArchivingStatus.PUBLISHED);

      Assertions.assertThat(updatedTestArchivalRecord.getPid())
          .isEqualTo(savedTestArchivalRecord.getPid());

      Assertions.assertThat(updatedTestArchivalRecord.getTimeStamp().truncatedTo(ChronoUnit.SECONDS)) // truncate because database doesn't save as exactly
          .isEqualTo(TEST_TIME.truncatedTo(ChronoUnit.SECONDS));

      Assertions.assertThat(updatedTestArchivalRecord.getExternalId())
          .isEqualTo(savedTestArchivalRecord.getExternalId());

    }

    @Test
    void throwsIfNoArchivalRecordsExist() throws Exception {

      // first delete test entry
      archivalRecordRepository.deleteAll();

      final String TEST_REQUEST_URL = String.format(
          "/api/curation/v1/projects/%s/objects/%s/archival-records/publish",
          testDataSet.project().getProjectAbbr(),
          testDataSet.digitalObject().getId()
      );

      final Instant TEST_TIME = Instant.now();
      final String TEST_REQUEST_BODY = String.format(
          "{\"timeStamp\":\"%s\"}",
          TEST_TIME
      );

      mockMvc.perform(
          MockMvcRequestBuilders.patch(TEST_REQUEST_URL)
              .contentType(MediaType.APPLICATION_JSON)
              .content(TEST_REQUEST_BODY)
      ).andExpect(status().is(400));

    }

  }

  @Nested
  class DELETE {

    @Test
    void successfullyDeletesTestArchivalRecord() throws Exception {

      final String TEST_REQUEST_URL = String.format(
          "/api/curation/v1/projects/%s/objects/%s/archival-records/%s",
          testDataSet.project().getProjectAbbr(),
          testDataSet.digitalObject().getId(),
          testDataSet.archivalRecord().getId()
      );

      mockMvc.perform(
          MockMvcRequestBuilders.delete(TEST_REQUEST_URL)
      ).andExpect(status().isOk());

      var expectedDeleted = archivalRecordRepository.findById(testDataSet.archivalRecord().getId());

      Assertions.assertThat(expectedDeleted).isEmpty();

    }

  }

}
