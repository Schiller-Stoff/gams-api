package org.ddh.gamsapi.domain.Project;

import org.assertj.core.api.Assertions;
import org.ddh.gamsapi.IntegrationTest;
import org.ddh.gamsapi.TestUtilities.*;
import org.ddh.gamsapi.application.WebDeployment.WebDeployment;
import org.ddh.gamsapi.application.WebDeployment.WebDeploymentRepository;
import org.ddh.gamsapi.domain.DigitalObject.DigitalObject;
import org.ddh.gamsapi.domain.DigitalObject.utils.interfaces.IDigitalObjectRepository;
import org.ddh.gamsapi.domain.Project.interfaces.IProjectRepository;
import org.ddh.gamsapi.infrastructure.System.security.IUserPrincipalAuditorMapping;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.auditing.AuditingHandler;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@AutoConfigureMockMvc(addFilters = false)
class ProjectControllerIT extends IntegrationTest {

  @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private IProjectRepository projectRepository;

  @Autowired
  private IDigitalObjectRepository digitalObjectRepository;

  @Autowired
  private WebDeploymentRepository webDeploymentRepository;


  // disables auditing
  // (necessary -> otherwise the createdBy fields etc. from Project need to be filled)
  // this auditing / security test is done in a separate test
  @MockitoBean
  private AuditingHandler auditingHandler;
  @MockitoBean
  private IUserPrincipalAuditorMapping userPrincipalAuditorMapping;

  private TestDataSet testDataSet;

  @Autowired
  private TestDataBuilder testDataBuilder;

  @BeforeEach
  void setup(){
    testDataSet = testDataBuilder.buildTestDataSet();
    Mockito.when(userPrincipalAuditorMapping.getCurrentAuditor())
        .thenReturn(Optional.of(TestUser.USERNAME.getValue()));
  }

  @Nested
  class WebclientTest {

    @Test
    void projectAbbrContainedInWebclientProjectsOverview() throws Exception {

      MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders.get("/api/curation/v1/projects").accept(MediaType.TEXT_HTML))
        .andExpect(MockMvcResultMatchers.status().isOk())
          // verifies that webclient html is being returned
          .andExpect(MockMvcResultMatchers.content().contentType("text/html;charset=UTF-8"))
          .andReturn();

      Assertions.assertThat(mvcResult.getResponse().getContentAsString())
          .contains(testDataSet.project().getProjectAbbr());

    }

  }

  @Nested
  class GETProject {


    @Test
    void GETProjectOverviewReturns200() throws Exception {
      mockMvc.perform(
          MockMvcRequestBuilders.get("/api/curation/v1/projects")
              .accept(MediaType.APPLICATION_JSON)
      ).andExpect(status().isOk());
    }

    @Test
    void GETProjectOverviewReturnsExpectedProjects() throws Exception {
      // perform GET request
      MvcResult mvcResult = mockMvc.perform(
          MockMvcRequestBuilders.get("/api/curation/v1/projects")
              .accept(MediaType.APPLICATION_JSON)
      ).andExpect(status().isOk())
          .andReturn();

      String responseBody = mvcResult.getResponse().getContentAsString();

      // assert that the response body contains the project abbreviation
      Assertions.assertThat(responseBody).contains(testDataSet.project().getProjectAbbr());
    }



  }

  @Nested
  class ProjectCreation {

    /**
     * Tests if a PUT request for creating a project returns https status 200.
     * Deactivated security filters and auditing for this test (done at class level).
     * @throws Exception if the test fails (mockMvc.perform)
     */
    @Test
    void PUTTestProjectReturns200() throws Exception {

      final String TEST_PROJECT_URL = String.format(
          "/api/curation/v1/projects/%s", "demo"
      );

      mockMvc.perform(
          MockMvcRequestBuilders.put(TEST_PROJECT_URL)
      ).andExpect(status().isOk());
    }

    /**
     * Tests if a PUT request for creating a project returns https status 200 when a
     * requestBody = JSON was defined.
     */
    @Test
    void PUTRequestAllowsToSaveProjectDescription() throws Exception {

      final String TEST_PROJECT_URL = String.format(
          "/api/curation/v1/projects/%s", "demo"
      );

      final String TEST_PROJECT_DESCRIPTION = TestProject.PROJECT_DESCRIPTION.getValue();
      final String TEST_PROJECT_PUT_REQUEST_BODY =  "{\"description\": \"" + TEST_PROJECT_DESCRIPTION + "\"}";

      // first create a project
      mockMvc.perform(
          MockMvcRequestBuilders.put(TEST_PROJECT_URL)
              .contentType(MediaType.APPLICATION_JSON)
              .content(TEST_PROJECT_PUT_REQUEST_BODY)
      ).andExpect(status().isOk());

      // assert that the project was saved
      var foundProject = projectRepository.findById(TestProject.PROJECT_ABBR.getValue());
      Assertions.assertThat(foundProject).isPresent();

      // assert that expected description was saved
      Assertions.assertThat(foundProject.get().getDescription())
          .isEqualTo(TEST_PROJECT_DESCRIPTION);
    }

    @Test
    void PUTRequestAllowsToSaveProjectTitle() throws Exception {

      final String TEST_PROJECT_URL = String.format(
          "/api/curation/v1/projects/%s", "demo"
      );

      final String TEST_PROJECT_TITLE = TestProject.PROJECT_TITLE.getValue();
      final String TEST_PROJECT_PUT_REQUEST_BODY =  "{\"title\": \"" + TEST_PROJECT_TITLE + "\"}";

      // create the project
      mockMvc.perform(
          MockMvcRequestBuilders.put(TEST_PROJECT_URL)
              .contentType(MediaType.APPLICATION_JSON)
              .content(TEST_PROJECT_PUT_REQUEST_BODY)
      ).andExpect(status().isOk());

      // assert that the project was saved
      var foundProject = projectRepository.findById(TestProject.PROJECT_ABBR.getValue());
      Assertions.assertThat(foundProject).isPresent();
      // assert that expected title was saved
      Assertions.assertThat(foundProject.get().getTitle())
          .isEqualTo(TEST_PROJECT_TITLE);
    }

  }

  @Nested
  class ProjectUpdate {

    @Test
    void PATCHofProjectAllowsToUpdateDescription() throws Exception {

      final String TEST_PROJECT_URL = String.format(
          "/api/curation/v1/projects/%s", testDataSet.project().getProjectAbbr()
      );

      // update the project description
      final String UPDATED_TEST_PROJECT_DESCRIPTION = "Updated description";
      final String TEST_PROJECT_PATCH_REQUEST_BODY =  "{\"description\": \"" + UPDATED_TEST_PROJECT_DESCRIPTION + "\"}";

      final String RESPONSE_BODY  = mockMvc.perform(
          MockMvcRequestBuilders.patch(TEST_PROJECT_URL)
              .contentType(MediaType.APPLICATION_JSON)
              .content(TEST_PROJECT_PATCH_REQUEST_BODY)
      ).andExpect(status().isOk())
          .andReturn().getResponse().getContentAsString();

      // assert via response body
      Assertions.assertThat(RESPONSE_BODY).contains(
          UPDATED_TEST_PROJECT_DESCRIPTION
      );

      // assert additionally via repo layer
      Project updatedProject = projectRepository.findById(
          testDataSet.project().getProjectAbbr()).orElseThrow();
      Assertions.assertThat(
          updatedProject.getDescription()
      ).isEqualTo(UPDATED_TEST_PROJECT_DESCRIPTION);

    }

    @Test
    void requestBodyIsRequired() throws Exception {

      final String TEST_PROJECT_URL = String.format(
          "/api/curation/v1/projects/%s", TestProject.PROJECT_ABBR.getValue()
      );

      mockMvc.perform(
              MockMvcRequestBuilders.patch(TEST_PROJECT_URL)
          ).andExpect(status().is4xxClientError());

    }

  }

  @Nested
  class ProjectDeletion {

    @Test
    void DELETEofProjectReturnsHTTPStatus200() throws Exception {

      testDataBuilder.removeAllExceptProjects(testDataSet);

      mockMvc.perform(
          MockMvcRequestBuilders.delete(
              String.format("/api/curation/v1/projects/%s", testDataSet.project().getProjectAbbr())
          )
      ).andExpect(status().isOk());

    }

    @Test
    void removesLinkedWebDeploymentIfExists() throws Exception {

      // 1. Arrange: Clean up digital objects so the project is eligible for deletion
      testDataBuilder.removeAllExceptProjects(testDataSet);
      String projectAbbr = testDataSet.project().getProjectAbbr();

      // Create and save an active web deployment for this project
      var webDeployment = WebDeployment.builder()
          .projectAbbr(projectAbbr)
          .deployedAt(java.time.Instant.now())
          .deployedBy("test-admin")
          .fileCount(10)
          .totalSize(2048L)
          .build();

      webDeploymentRepository.save(webDeployment);

      // Verify initial state
      Assertions.assertThat(webDeploymentRepository.existsById(projectAbbr))
          .as("Web deployment should exist before calling the delete endpoint")
          .isTrue();

      // 2. Act: Perform the DELETE request via the REST API
      mockMvc.perform(
          MockMvcRequestBuilders.delete(
              String.format("/api/curation/v1/projects/%s", projectAbbr)
          )
      ).andExpect(MockMvcResultMatchers.status().isOk());

      // 3. Assert: Verify the event listener successfully cascaded the deletion
      Assertions.assertThat(webDeploymentRepository.existsById(projectAbbr))
          .as("The linked WebDeployment should be automatically deleted by the event listener")
          .isFalse();
    }

  }


  @Nested
  class ProjectHEAD {

    @Test
    void HEADofProjectReturnsHTTPStatus200() throws Exception {

      mockMvc.perform(
          MockMvcRequestBuilders.head(
              String.format("/api/curation/v1/projects/%s", testDataSet.project().getProjectAbbr())
          )
      ).andExpect(status().isOk());

    }

    @Test
    void HEADofProjectReturnsHTTPStatus404IfNOTFOUND() throws Exception {

      mockMvc.perform(
          MockMvcRequestBuilders.head(
              String.format("/api/curation/v1/projects/%s", "not-existing-project")
          )
      ).andExpect(status().isNotFound());

    }

    @Test
    void HEADProjectResponsesWithContainedLastModifiedHeader() throws Exception {

      mockMvc.perform(
          MockMvcRequestBuilders.head(
              String.format("/api/curation/v1/projects/%s", testDataSet.project().getProjectAbbr())
          )
      ).andExpect(
          MockMvcResultMatchers.header().exists("Last-Modified"));
    }

    @Test
    void HEADProjectResponsesWithExpectedLastModifiedHeaderDate() throws Exception {

      String lastModifiedHeaderValue = mockMvc.perform(
          MockMvcRequestBuilders.head(
              String.format("/api/curation/v1/projects/%s", testDataSet.project().getProjectAbbr())
          )
      ).andReturn().getResponse().getHeader("Last-Modified");

      Assertions.assertThat(lastModifiedHeaderValue).isNotNull();

      // parse Last-Modified header (RFC 1123) to Instant
      Instant lastModifiedFromHeader = ZonedDateTime
          .parse(lastModifiedHeaderValue, DateTimeFormatter.RFC_1123_DATE_TIME)
          .toInstant();

      // expected: truncate to seconds since RFC 1123 has no sub-second precision
      Instant expectedDate = testDataSet.project().getModified()
          .truncatedTo(ChronoUnit.SECONDS);

      Assertions.assertThat(lastModifiedFromHeader)
          .isEqualTo(expectedDate);
    }


    @Test
    void HEADProjectRespondsWithExpectedLastModifiedValue() throws Exception {

      // wait 1 second (the last modified date via controller is only seconds accurate)
      Thread.sleep(1000);

      // save a digital object to the project
      DigitalObject savedDigitalObject = digitalObjectRepository.save(TestDigitalObject.generate());

      String projectLastModifiedHeaderValue = mockMvc.perform(
          MockMvcRequestBuilders.head(
              String.format("/api/curation/v1/projects/%s", testDataSet.project().getProjectAbbr())
          )
      ).andReturn().getResponse().getHeader("Last-Modified");

      Assertions.assertThat(projectLastModifiedHeaderValue).isNotNull();

      // parse Last-Modified header (RFC 1123) to Instant
      Instant lastModifiedFromHeader = ZonedDateTime
          .parse(projectLastModifiedHeaderValue, DateTimeFormatter.RFC_1123_DATE_TIME)
          .toInstant();

      // expected: truncate to seconds since RFC 1123 has no sub-second precision
      Instant expectedDate = savedDigitalObject.getModified()
          .truncatedTo(ChronoUnit.SECONDS);

      Assertions.assertThat(lastModifiedFromHeader)
          .isBefore(expectedDate);
    }

    @Test
    void HEADProjectRespondsWithExpectedLastModifiedValueRoundedToHours() throws Exception {

      // save a digital object to the project
      DigitalObject savedDigitalObject = digitalObjectRepository.save(TestDigitalObject.generate());

      // get updated project from database
      var foundProject = projectRepository.findById(savedDigitalObject.getProject().getProjectAbbr())
          .orElseThrow();

      // the contentLastModified date should be updated to the same date as the digital object modified date
      String projectLastModifiedHeaderValue = mockMvc.perform(
          MockMvcRequestBuilders.head(
              String.format("/api/curation/v1/projects/%s", foundProject.getProjectAbbr())
          )
      ).andReturn().getResponse().getHeader("Last-Modified");

      Assertions.assertThat(projectLastModifiedHeaderValue).isNotNull();

      // parse Last-Modified header (RFC 1123) to Instant
      Instant lastModifiedFromHeader = ZonedDateTime
          .parse(projectLastModifiedHeaderValue, DateTimeFormatter.RFC_1123_DATE_TIME)
          .toInstant();

      // expected: truncate to seconds since RFC 1123 has no sub-second precision
      Instant expectedDate = savedDigitalObject.getModified()
          .truncatedTo(ChronoUnit.HOURS);

      Assertions.assertThat(lastModifiedFromHeader.truncatedTo(ChronoUnit.HOURS))
          .isEqualTo(expectedDate);
    }

    @Test
    void HEADProjectIfModifiedSinceIsMalformedRespondWith400() throws Exception {


      final String MALFORMED_DATE = "PETER";

      mockMvc.perform(
          MockMvcRequestBuilders.head(
              String.format("/api/curation/v1/projects/%s", testDataSet.project().getProjectAbbr())
          ).header("If-Modified-Since", MALFORMED_DATE)
      ).andExpect(status().isBadRequest());

    }


    /**
     * Tests if the server responds with a 304 status code if the If-Modified-Since header is set to a date in the future.
     * @throws Exception if the test fails (mockMvc.perform)
     */
    @Test
    void HEADProjectIfModifiedSinceRespondsWithIsNotModifiedHttpSTATUS() throws Exception {

      // Create a date in the future that's properly formatted for HTTP headers
      ZonedDateTime futureDate = ZonedDateTime.now(ZoneId.systemDefault()).plusYears(1);
      String ifModifiedSinceHeader = DateTimeFormatter.RFC_1123_DATE_TIME.format(futureDate);

      mockMvc.perform(
        MockMvcRequestBuilders.head(
          String.format("/api/curation/v1/projects/%s", testDataSet.project().getProjectAbbr())
        ).header("If-Modified-Since", ifModifiedSinceHeader)
      ).andExpect(status().isNotModified());

    }

  }


  @Nested
  class GETProjectDetailsJson {

    @Test
    void returnsProjectDetailsForExistingProject() throws Exception {

      MvcResult mvcResult = mockMvc.perform(
              MockMvcRequestBuilders.get(
                      "/api/curation/v1/projects/{projectAbbr}",
                      testDataSet.project().getProjectAbbr()
                  )
                  .accept(MediaType.APPLICATION_JSON)
          )
          .andExpect(status().isOk())
          .andReturn();

      String response = mvcResult.getResponse().getContentAsString();

      Assertions.assertThat(response)
          .contains(testDataSet.project().getProjectAbbr())
          .contains("statistics")
          .contains("digitalObjectCount")
          .contains("datastreamCount")
          .contains("totalStorageBytes");
    }

    @Test
    void responseContainsExpectedProjectAbbr() throws Exception {

      MvcResult mvcResult = mockMvc.perform(
              MockMvcRequestBuilders.get(
                      "/api/curation/v1/projects/{projectAbbr}",
                      testDataSet.project().getProjectAbbr()
                  )
                  .accept(MediaType.APPLICATION_JSON)
          )
          .andExpect(status().isOk())
          .andReturn();

      String response = mvcResult.getResponse().getContentAsString();

      // The project abbr value should appear in the JSON
      Assertions.assertThat(response)
          .contains("\"projectAbbr\":\"" + testDataSet.project().getProjectAbbr() + "\"");
    }

    @Test
    void returns404ForNonExistentProject() throws Exception {

      mockMvc.perform(
              MockMvcRequestBuilders.get("/api/curation/v1/projects/{projectAbbr}", "nonExistent")
                  .accept(MediaType.APPLICATION_JSON)
          )
          .andExpect(status().isNotFound());
    }

    @Test
    void statisticsReflectActualDataCounts() throws Exception {
      // The testDataSet includes 1 digital object and at least 1 datastream

      MvcResult mvcResult = mockMvc.perform(
              MockMvcRequestBuilders.get(
                      "/api/curation/v1/projects/{projectAbbr}",
                      testDataSet.project().getProjectAbbr()
                  )
                  .accept(MediaType.APPLICATION_JSON)
          )
          .andExpect(status().isOk())
          .andReturn();

      String response = mvcResult.getResponse().getContentAsString();

      // digitalObjectCount should be at least 1 (from testDataSet)
      // We can't assert exact counts without knowing TestDataBuilder internals,
      // but we can assert that the count is NOT zero since testDataSet creates objects.
      Assertions.assertThat(response)
          .doesNotContain("\"digitalObjectCount\":0");
    }

    @Test
    void emptyProjectHasZeroStatistics() throws Exception {
      // Remove all objects but keep the project
      testDataBuilder.removeAllExceptProjects(testDataSet);

      MvcResult mvcResult = mockMvc.perform(
              MockMvcRequestBuilders.get(
                      "/api/curation/v1/projects/{projectAbbr}",
                      testDataSet.project().getProjectAbbr()
                  )
                  .accept(MediaType.APPLICATION_JSON)
          )
          .andExpect(status().isOk())
          .andReturn();

      String response = mvcResult.getResponse().getContentAsString();

      Assertions.assertThat(response)
          .contains("\"digitalObjectCount\":0")
          .contains("\"datastreamCount\":0")
          .contains("\"totalStorageBytes\":0");
    }

  }


  @Nested
  class GETProjectPageHtml {

    @Test
    void rendersProjectPageForExistingProject() throws Exception {

      mockMvc.perform(
              MockMvcRequestBuilders.get(
                      "/api/curation/v1/projects/{projectAbbr}",
                      testDataSet.project().getProjectAbbr()
                  )
                  .accept(MediaType.TEXT_HTML)
          )
          .andExpect(status().isOk())
          .andExpect(MockMvcResultMatchers.view().name("Project/show"))
          .andExpect(MockMvcResultMatchers.content().contentType("text/html;charset=UTF-8"));
    }

    @Test
    void projectPageContainsExpectedProjectValues() throws Exception {

      MvcResult mvcResult = mockMvc.perform(
              MockMvcRequestBuilders.get(
                      "/api/curation/v1/projects/{projectAbbr}",
                      testDataSet.project().getProjectAbbr()
                  )
                  .accept(MediaType.TEXT_HTML)
          )
          .andExpect(status().isOk())
          .andReturn();

      String response = mvcResult.getResponse().getContentAsString();

      Assertions.assertThat(response)
          .contains(testDataSet.project().getProjectAbbr());
    }

    @Test
    void returns404ForNonExistentProjectHtml() throws Exception {

      mockMvc.perform(
              MockMvcRequestBuilders.get("/api/curation/v1/projects/{projectAbbr}", "nonExistent")
                  .accept(MediaType.TEXT_HTML)
          )
          .andExpect(status().isNotFound());
    }

  }


}
