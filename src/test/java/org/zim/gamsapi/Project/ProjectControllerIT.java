package org.zim.gamsapi.Project;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.auditing.AuditingHandler;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.zim.gamsapi.DigitalObject.DigitalObject;
import org.zim.gamsapi.DigitalObject.DublinCoreEntry.DublinCoreEntry;
import org.zim.gamsapi.DigitalObject.DublinCoreEntry.IDublinCoreEntryRepository;
import org.zim.gamsapi.DigitalObject.IDigitalObjectRepository;
import org.zim.gamsapi.IntegrationTest;
import org.zim.gamsapi.Project.interfaces.IProjectRepository;
import org.zim.gamsapi.enums.TestDigitalObject;
import org.zim.gamsapi.enums.TestDublinCoreEntry;
import org.zim.gamsapi.enums.TestProject;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@AutoConfigureMockMvc(addFilters = false)
public class ProjectControllerIT extends IntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private IProjectRepository projectRepository;

  @Autowired
  private IDigitalObjectRepository digitalObjectRepository;

  @Autowired
  private IDublinCoreEntryRepository dublinCoreEntryRepository;

  // disables auditing
  // (necessary -> otherwise the createdBy fields etc. from Project need to be filled)
  // this auditing / security test is done in a separate test
  @MockBean
  private AuditingHandler auditingHandler;


  @Nested
  public class WebclientTest {

    @Test
    public void projectAbbrContainedInWebclientProjectsOverview() throws Exception {

      Project project = ProjectBuilder.builder().projectAbbr(TestProject.PROJECT_ABBR.getValue()).build();

      projectRepository.save(project);

      MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/projects/").accept(MediaType.TEXT_HTML))
        .andExpect(MockMvcResultMatchers.status().isOk())
          // verifies that webclient html is being returned
          .andExpect(MockMvcResultMatchers.content().contentType("text/html;charset=UTF-8"))
          .andReturn();

      Assertions.assertThat(mvcResult.getResponse().getContentAsString())
          .contains(project.getProjectAbbr());

    }

  }

  @Nested
  public class ProjectCreation {

    /**
     * Tests if a PUT request for creating a project returns https status 200.
     * Deactivated security filters and auditing for this test (done at class level).
     * @throws Exception if the test fails (mockMvc.perform)
     */
    @Test
    public void PUTTestProjectReturns200() throws Exception {

      final String TEST_PROJECT_URL = String.format(
          "/api/v1/projects/%s", TestProject.PROJECT_ABBR.getValue()
      );

      // first create a project
      mockMvc.perform(
          MockMvcRequestBuilders.put(TEST_PROJECT_URL)
      ).andExpect(status().isOk());

    }

  }

  @Nested
  public class ProjectDeletion {

    @Test
    public void DELETEofProjectReturnsHTTPStatus200() throws Exception {

      projectRepository.save(TestProject.generate());

      mockMvc.perform(
          MockMvcRequestBuilders.delete(
              String.format("/api/v1/projects/%s", TestProject.PROJECT_ABBR.getValue())
          )
      ).andExpect(status().isOk());

    }

  }


  @Nested
  public class ProjectHEAD {

    @Test
    public void HEADofProjectReturnsHTTPStatus200() throws Exception {

      Project savedTestProject = projectRepository.save(TestProject.generate());

      mockMvc.perform(
          MockMvcRequestBuilders.head(
              String.format("/api/v1/projects/%s", savedTestProject.getProjectAbbr())
          )
      ).andExpect(status().isOk());

    }

    @Test
    public void HEADofProjectReturnsHTTPStatus404IfNOTFOUND() throws Exception {

      mockMvc.perform(
          MockMvcRequestBuilders.head(
              String.format("/api/v1/projects/%s", TestProject.PROJECT_ABBR.getValue())
          )
      ).andExpect(status().isNotFound());

    }

    @Test
    public void HEADProjectResponsesWithContainedLastModifiedHeader() throws Exception {

      Project savedProject = projectRepository.save(TestProject.generate());

      mockMvc.perform(
          MockMvcRequestBuilders.head(
              String.format("/api/v1/projects/%s", savedProject.getProjectAbbr())
          )
      ).andExpect(
          MockMvcResultMatchers.header().exists("Last-Modified"));
    }

    @Test
    public void HEADProjectResponsesWithExpectedLastModifiedHeaderDate() throws Exception {

      Project savedProject = projectRepository.save(TestProject.generate());

      String lastModifiedHeaderValue = mockMvc.perform(
          MockMvcRequestBuilders.head(
              String.format("/api/v1/projects/%s", savedProject.getProjectAbbr())
          )
      ).andReturn().getResponse().getHeader("Last-Modified");

      Assertions.assertThat(lastModifiedHeaderValue).isNotNull();

      // parse lastModified to Date
      DateTimeFormatter formatter = DateTimeFormatter.RFC_1123_DATE_TIME;
      ZonedDateTime zonedDateTime = ZonedDateTime.parse(lastModifiedHeaderValue, formatter);
      ZonedDateTime localZonedDateTime = zonedDateTime.withZoneSameInstant(ZoneId.systemDefault());
      Date lastModifiedHeaderValueAsDate = Date.from(localZonedDateTime.toInstant());

      // expected date
      Date expectedDate = savedProject.getModified();
      // remove milliseconds (the database works with milliseconds but the header does not - because of ISO RFC 1123)
      expectedDate.setTime(expectedDate.getTime() / 1000 * 1000);


      Assertions.assertThat(lastModifiedHeaderValueAsDate).hasSameTimeAs(expectedDate);

    }


    @Test
    public void HEADProjectRespondsWithLastModifiedOfLastDigitalObjectCreated() throws Exception {

      Project savedProject = projectRepository.save(TestProject.generate());

      // wait 1 second (the last modified date via controller is only seconds accurate)
      Thread.sleep(1000);

      DigitalObject savedDigitalObject = digitalObjectRepository.save(TestDigitalObject.generate());

      String lastModifiedHeaderValue = mockMvc.perform(
          MockMvcRequestBuilders.head(
              String.format("/api/v1/projects/%s", savedProject.getProjectAbbr())
          )
      ).andReturn().getResponse().getHeader("Last-Modified");

      Assertions.assertThat(lastModifiedHeaderValue).isNotNull();

      // parse lastModified to Date
      DateTimeFormatter formatter = DateTimeFormatter.RFC_1123_DATE_TIME;
      ZonedDateTime zonedDateTime = ZonedDateTime.parse(lastModifiedHeaderValue, formatter);
      ZonedDateTime localZonedDateTime = zonedDateTime.withZoneSameInstant(ZoneId.systemDefault());
      Date lastModifiedHeaderValueAsDate = Date.from(localZonedDateTime.toInstant());

      // expected date
      Date expectedDate = savedDigitalObject.getModified();
      // remove milliseconds (the database works with milliseconds but the header does not - because of ISO RFC 1123)
      expectedDate.setTime(expectedDate.getTime() / 1000 * 1000);

      Assertions.assertThat(lastModifiedHeaderValueAsDate).hasSameTimeAs(expectedDate);

      // assert that it is not the same as saved project modified (because the digital object was created after the project)
      Date notExpectedDate = savedProject.getModified();
      // remove milliseconds
      notExpectedDate.setTime(notExpectedDate.getTime() / 1000 * 1000);
      Assertions.assertThat(lastModifiedHeaderValueAsDate).doesNotHaveToString(notExpectedDate.toString());

    }

    @Test
    public void HEADProjectIfModifiedSinceIsMalformedRespondWith400() throws Exception {

      Project savedProject = projectRepository.save(TestProject.generate());

      final String MALFORMED_DATE = "PETER";

      mockMvc.perform(
          MockMvcRequestBuilders.head(
              String.format("/api/v1/projects/%s", savedProject.getProjectAbbr())
          ).header("If-Modified-Since", MALFORMED_DATE)
      ).andExpect(status().isBadRequest());

    }


    /**
     * Tests if the server responds with a 304 status code if the If-Modified-Since header is set to a date in the future.
     * @throws Exception if the test fails (mockMvc.perform)
     */
    @Test
    public void HEADProjectIfModifiedSinceRespondsWithIsNotModifiedHttpSTATUS() throws Exception {

      Project savedProject = projectRepository.save(TestProject.generate());

      // Create a date in the future that's properly formatted for HTTP headers
      ZonedDateTime futureDate = ZonedDateTime.now(ZoneId.systemDefault()).plusYears(1);
      String ifModifiedSinceHeader = DateTimeFormatter.RFC_1123_DATE_TIME.format(futureDate);

      mockMvc.perform(
        MockMvcRequestBuilders.head(
          String.format("/api/v1/projects/%s", savedProject.getProjectAbbr())
        ).header("If-Modified-Since", ifModifiedSinceHeader)
      ).andExpect(status().isNotModified());

    }

  }


  @Nested
  public class DublinCoreSearch {

    Project testProject = TestProject.generate();
    DigitalObject testDigitalObject = TestDigitalObject.generate();
    DublinCoreEntry testDublinCoreEntry = TestDublinCoreEntry.generate(testDigitalObject.getId());

    final String CONTAINS_SEARCH_URL_TEMPLATE = "/api/v1/projects/search/dc?projectAbbrs=%s&dcEntryName=%s&contains=%s";
    final String MATCHES_SEARCH_URL_TEMPLATE = "/api/v1/projects/search/dc?projectAbbrs=%s&dcEntryName=%s&matches=%s";

    @BeforeEach
    public void setup() {
      projectRepository.save(testProject);
      digitalObjectRepository.save(testDigitalObject);
      dublinCoreEntryRepository.save(testDublinCoreEntry);
    }

    // TODO rename test to clarify what it's doing!
    @Test
    public void GETDublinCoreEntryWithContainsReturnsExpectedTestObject() throws Exception {
      String requestUrl = String.format(
          CONTAINS_SEARCH_URL_TEMPLATE,
          testProject.getProjectAbbr(),
          testDublinCoreEntry.getName(),
          testDublinCoreEntry.getValue()
      );

      String response = mockMvc.perform(
          MockMvcRequestBuilders.get(requestUrl)
      ).andExpect(status().isOk())
          .andReturn()
          .getResponse()
          .getContentAsString();

      Assertions.assertThat(response)
          .contains(
              testDigitalObject.getId(),
              testDigitalObject.getProject().getProjectAbbr(),
              testDigitalObject.getBaseMetadata().getTitle(),
              testDigitalObject.getBaseMetadata().getDescription()
          );

    }

    @Test
    public void GETDublinCoreEntryWithMatchesReturnsExpectedTestObject() throws Exception {
      String requestUrl = String.format(
          MATCHES_SEARCH_URL_TEMPLATE,
          testProject.getProjectAbbr(),
          testDublinCoreEntry.getName(),
          testDublinCoreEntry.getValue()
      );

      String response = mockMvc.perform(
          MockMvcRequestBuilders.get(requestUrl)
      ).andExpect(status().isOk())
          .andReturn()
          .getResponse()
          .getContentAsString();

      Assertions.assertThat(response)
          .contains(
              testDigitalObject.getId(),
              testDigitalObject.getProject().getProjectAbbr(),
              testDigitalObject.getBaseMetadata().getTitle(),
              testDigitalObject.getBaseMetadata().getDescription()
          );

    }

    @Test
    public void returnsErrorIfMatchesAndContainsNotDefined() throws Exception {
      final String MALFORMED_URL = "/api/v1/projects/search/dc?projectAbbrs=%s";
      mockMvc.perform(
          MockMvcRequestBuilders.get(MALFORMED_URL)
      ).andExpect(
          status().isBadRequest()
      );


    }

  }

}
