package org.zim.gamsapi.DigitalObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.auditing.AuditingHandler;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.zim.gamsapi.Datastream.Datastream;
import org.zim.gamsapi.Datastream.IDatastreamRepository;
import org.zim.gamsapi.Datastream.interfaces.IDatastreamContentRepository;
import org.zim.gamsapi.DigitalObject.interfaces.DigitalObjectDetailsView;
import org.zim.gamsapi.IntegrationTest;
import org.zim.gamsapi.Project.Project;
import org.zim.gamsapi.Project.ProjectBuilder;
import org.zim.gamsapi.Project.interfaces.IProjectRepository;
import org.zim.gamsapi.enums.TestDatastream;
import org.zim.gamsapi.enums.TestDatastreamContent;
import org.zim.gamsapi.enums.TestDigitalObject;
import org.zim.gamsapi.enums.TestProject;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc(addFilters = false) // deactivates spring security for the test class
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class DigitalObjectControllerIT extends IntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private IProjectRepository projectRepository;

  @Autowired
  private IDigitalObjectRepository digitalObjectRepository;

  @Autowired
  private IDatastreamRepository datastreamRepository;

  @Autowired
  private IDatastreamContentRepository datastreamContentRepository;

  @MockBean
  private AuditingHandler auditingHandler;

  private Project testProject;

  @BeforeEach
  public void setup() {
    testProject = ProjectBuilder.builder().projectAbbr(TestProject.PROJECT_ABBR.getValue()).build();
    projectRepository.save(testProject);
  }

  @Nested
  public class PUTRequests {

    @Test
    public void createsDigitalObjectWithExpectedId() throws Exception {
      // Arrange
      DigitalObject expectedDigitalObject = TestDigitalObject.generate();
      ObjectMapper objectMapper = new ObjectMapper();
      String expectedDigitalObjectJson = objectMapper.writeValueAsString(expectedDigitalObject);

      // Act
      mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/projects/{projectAbbr}/objects/{id}", testProject.getProjectAbbr(), expectedDigitalObject.getId())
          .contentType(MediaType.APPLICATION_JSON)
          .content(expectedDigitalObjectJson))
          // PUT request will redirect to GET
          .andExpect(status().is3xxRedirection());

      // Assert - PUT digital object can be found via repository class
      org.assertj.core.api.Assertions.assertThat(
          digitalObjectRepository.findDigitalObjectById(expectedDigitalObject.getId()))
            .isPresent()
            .get()
            .isNotNull()
            .extracting(DigitalObjectDetailsView::getId)
            .isEqualTo(expectedDigitalObject.getId()
      );

    }


    @Test
    public void createsDigitalObjectWithExpectedProperties() throws Exception {

      // Arrange
      DigitalObject expectedDigitalObject = TestDigitalObject.generate();

      ObjectMapper objectMapper = new ObjectMapper();
      String expectedDigitalObjectJson = objectMapper.writeValueAsString(expectedDigitalObject);

      // Act
      mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/projects/{projectAbbr}/objects/{id}", testProject.getProjectAbbr(), expectedDigitalObject.getId())
              .contentType(MediaType.APPLICATION_JSON)
              .content(expectedDigitalObjectJson))
          // PUT request will redirect to GET
          .andExpect(status().is3xxRedirection());

      DigitalObjectDetailsView foundObject = digitalObjectRepository
          .findDigitalObjectById(expectedDigitalObject.getId()).orElseThrow(AssertionError::new);

      org.assertj.core.api.Assertions.assertThat(foundObject.getId()).isEqualTo(expectedDigitalObject.getId());
      org.assertj.core.api.Assertions.assertThat(foundObject.getMainResource()).isEqualTo(expectedDigitalObject.getMainResource());
      org.assertj.core.api.Assertions.assertThat(foundObject.getFunder()).isEqualTo(expectedDigitalObject.getFunder());
      org.assertj.core.api.Assertions.assertThat(foundObject.getPublisher()).isEqualTo(expectedDigitalObject.getPublisher());
      org.assertj.core.api.Assertions.assertThat(foundObject.getObjectType()).isEqualTo(expectedDigitalObject.getObjectType());
      org.assertj.core.api.Assertions.assertThat(foundObject.getBaseMetadata()).isEqualTo(expectedDigitalObject.getBaseMetadata());

      org.assertj.core.api.Assertions.assertThat(foundObject.getPublished()).isEqualTo(expectedDigitalObject.getPublished());


    }





  }


  @Nested
  public class DELETERequests {

    @Test
    public void deleteDigitalObjectWhenItExists() throws Exception {
      // Arrange
      DigitalObject digitalObject = TestDigitalObject.generate();

      digitalObjectRepository.save(digitalObject);

      // Act
      mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/projects/{projectAbbr}/objects/{id}", testProject.getProjectAbbr(), digitalObject.getId())
          .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().is3xxRedirection());

      // Assert
      org.assertj.core.api.Assertions.assertThat(
          digitalObjectRepository.findDigitalObjectById(digitalObject.getId()))
            .isNotPresent();

    }

    @Test
    public void deleteDigitalObjectWhenItContainsDatastreams() throws Exception {
      // Arrange
      DigitalObject digitalObject = TestDigitalObject.generate();

      digitalObjectRepository.save(digitalObject);

      Datastream datastream = TestDatastream.generate(digitalObject);

      datastreamRepository.save(datastream);
      datastreamContentRepository.save(TestDatastreamContent.CONTENT.getValue().getBytes(), datastream.deriveDatastreamId());


      // Act
      mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/projects/{projectAbbr}/objects/{id}", testProject.getProjectAbbr(), digitalObject.getId())
              .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().is3xxRedirection());

      // Assert
      org.assertj.core.api.Assertions.assertThat(
              digitalObjectRepository.findDigitalObjectById(digitalObject.getId()))
          .isNotPresent();

      org.assertj.core.api.Assertions.assertThat(
              datastreamRepository.findById(datastream.deriveDatastreamId()))
          .isNotPresent();

      // assert that the datastream content has been deleted
      org.assertj.core.api.Assertions.assertThat(datastreamContentRepository.exists(datastream.deriveDatastreamId())).isFalse();

    }

  }

  @Nested
  public class HEADRequests {

    @Test
    public void headDigitalObjectReturns200ifObjectExists() throws Exception {
      // Arrange
      DigitalObject digitalObject = TestDigitalObject.generate();

      digitalObjectRepository.save(digitalObject);

      // Act
      mockMvc.perform(MockMvcRequestBuilders.head("/api/v1/projects/{projectAbbr}/objects/{id}",
                  testProject.getProjectAbbr(),
                  digitalObject.getId())
          .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk());

    }

    @Test
    public void headDigitalObjectReturns404WhenObjectDoesNotExist() throws Exception {
      // Act
      mockMvc.perform(MockMvcRequestBuilders.head("/api/v1/projects/{projectAbbr}/objects/{id}",
                  testProject.getProjectAbbr(),
                  "nonExistentId")
          .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isNotFound());

    }

    @Test
    public void HEADDigitalObjectResponsesWithIncludedLastModifiedHeader() throws Exception {

      // Arrange
      DigitalObject digitalObject = TestDigitalObject.generate();
      digitalObjectRepository.save(digitalObject);

      // assert
      mockMvc.perform(
          MockMvcRequestBuilders.head(
              "/api/v1/projects/{projectAbbr}/objects/{id}", testProject.getProjectAbbr(), digitalObject.getId()
          )
      ).andExpect(
          MockMvcResultMatchers.header().exists("Last-Modified"));


    }

    /**
     * Tests if the Last-Modified header of a HEAD request contains the expected date
     * (of the saved digital object).
     * @throws Exception if the test fails (mockMvc.perform)
     */
    @Test
    public void HEADDigitalObjectResponsesWithExpectedLastModifiedHeaderDate() throws Exception {

      // Arrange
      DigitalObject digitalObject = TestDigitalObject.generate();
      digitalObject = digitalObjectRepository.save(digitalObject);

      // Act
      String lastModifiedHeaderValue = mockMvc.perform(
          MockMvcRequestBuilders.head(
              "/api/v1/projects/{projectAbbr}/objects/{id}", testProject.getProjectAbbr(), digitalObject.getId()
          )
      ).andReturn().getResponse().getHeader("Last-Modified");

      // Assert
      org.assertj.core.api.Assertions.assertThat(lastModifiedHeaderValue).isNotNull();

      // parse lastModified to Date
      DateTimeFormatter formatter = DateTimeFormatter.RFC_1123_DATE_TIME;
      ZonedDateTime zonedDateTime = ZonedDateTime.parse(lastModifiedHeaderValue, formatter);
      ZonedDateTime localZonedDateTime = zonedDateTime.withZoneSameInstant(ZoneId.systemDefault());
      Date lastModifiedHeaderValueAsDate = Date.from(localZonedDateTime.toInstant());

      // expected date
      Date expectedDate = digitalObject.getModified();
      // remove milliseconds (the database works with milliseconds but the header does not - because of ISO RFC 1123)
      expectedDate.setTime(expectedDate.getTime() / 1000 * 1000);

      // assert same time
      org.assertj.core.api.Assertions.assertThat(lastModifiedHeaderValueAsDate).hasSameTimeAs(expectedDate);

    }

    /**
     * If a client supplies a If-Modified-Since header wit an invalid date format,
     * the server should respond with a 400 Bad Request status.
     * @throws Exception if the test fails (mockMvc.perform)
     */
    @Test
    public void HEADProjectIfModifiedSinceIsMalformedRespondWith400() throws Exception {

      DigitalObject savedObject = digitalObjectRepository.save(
          TestDigitalObject.generate(testProject.getProjectAbbr())
      );

      final String MALFORMED_DATE = "PETER";

      final String URL = String.format("/api/v1/projects/%s/objects/%s", savedObject.getProject().getProjectAbbr(), savedObject.getId());

      mockMvc.perform(
          MockMvcRequestBuilders
              .head(URL)
              .header("If-Modified-Since", MALFORMED_DATE)
      ).andExpect(
          status().isBadRequest()
      );

    }

    /**
     * If a client supplies a If-Modified-Since header with a date that is after the last modified date of the object,
     * the server should respond with a 304 Not Modified status.
     * @throws Exception if the test fails (mockMvc.perform)
     */
    @Test
    public void HEADProjectIfModifiedSinceRespondsWithIsNotModifiedHttpSTATUS() throws Exception {

      DigitalObject savedObject = digitalObjectRepository.save(
          TestDigitalObject.generate(testProject.getProjectAbbr())
      );

      // Create a date in the future that's properly formatted for HTTP headers
      ZonedDateTime futureDate = ZonedDateTime.now(ZoneId.systemDefault()).plusYears(1);
      String ifModifiedSinceHeader = DateTimeFormatter.RFC_1123_DATE_TIME.format(futureDate);

      final String URL = String.format("/api/v1/projects/%s/objects/%s", savedObject.getProject().getProjectAbbr(), savedObject.getId());

      mockMvc.perform(
          MockMvcRequestBuilders
              .head(URL)
              .header("If-Modified-Since", ifModifiedSinceHeader)
      ).andExpect(
          status().isNotModified()
      );

    }


  }

  @Nested
  public class WebclientTests {



    @Test
    public void getDigitalObjectRendersExpectedViewValues() throws Exception {

      DigitalObject digitalObject = TestDigitalObject.generate();
      digitalObject.setObjectType("testObjectType");

      digitalObjectRepository.save(digitalObject);

      String url = String.format("/api/v1/projects/%s/objects/%s", testProject.getProjectAbbr(), digitalObject.getId());

      MvcResult mvcResult = mockMvc.perform(
            MockMvcRequestBuilders.get(url)
                .accept(MediaType.TEXT_HTML)
                .contentType(MediaType.TEXT_HTML)
          )
          .andExpect(status().isOk())
          .andExpect(MockMvcResultMatchers.view().name("DigitalObject/show"))
          .andExpect(MockMvcResultMatchers.content().contentType("text/html;charset=UTF-8"))
          .andReturn();

      String response = mvcResult.getResponse().getContentAsString();
      org.assertj.core.api.Assertions.assertThat(response)
          .contains(
              digitalObject.getId(),
              digitalObject.getProject().getProjectAbbr(),
              digitalObject.getObjectType(),
              digitalObject.getFunder(),
              digitalObject.getPublisher(),
              digitalObject.getMainResource()
          );

      //match expected datastream id ONLY ONCE! (because datastreams are not created)
      org.assertj.core.api.Assertions.assertThat(response).containsPattern(
          String.format("(%s.*?){1}", TestDigitalObject.DIGITAL_OBJECT_MAIN_RESOURCE.getValue())
      );


    }


    @Test
    public void digitalObjectShowsExpectedDatastreamDsids() throws Exception {

      DigitalObject digitalObject = TestDigitalObject.generate();

      digitalObjectRepository.save(digitalObject);

      Datastream datastream = TestDatastream.generate(digitalObject, "testDsId.xml");
      Datastream datastream2 = TestDatastream.generate(digitalObject, "testDsId2.xml");

      datastreamRepository.save(datastream);
      datastreamRepository.save(datastream2);

      String url = String.format("/api/v1/projects/%s/objects/%s", testProject.getProjectAbbr(), digitalObject.getId());

      MvcResult mvcResult = mockMvc.perform(
            MockMvcRequestBuilders.get(url)
                .accept(MediaType.TEXT_HTML)
                .contentType(MediaType.TEXT_HTML)
          )
          .andExpect(status().isOk())
          .andExpect(MockMvcResultMatchers.view().name("DigitalObject/show"))
          .andExpect(MockMvcResultMatchers.content().contentType("text/html;charset=UTF-8"))
          .andReturn();

      org.assertj.core.api.Assertions.assertThat(mvcResult.getResponse().getContentAsString())
          .contains(
              datastream.getDsid(),
              datastream2.getDsid()
          );

    }

    @Test
    public void getDigitalObjectRendersExpectedBaseMetadata() throws Exception {

      DigitalObject digitalObject = TestDigitalObject.generate();

      digitalObjectRepository.save(digitalObject);

      String url = String.format("/api/v1/projects/%s/objects/%s", testProject.getProjectAbbr(), digitalObject.getId());

      MvcResult mvcResult = mockMvc.perform(
            MockMvcRequestBuilders.get(url)
                .accept(MediaType.TEXT_HTML)
                .contentType(MediaType.TEXT_HTML)
          )
          .andExpect(status().isOk())
          .andExpect(MockMvcResultMatchers.view().name("DigitalObject/show"))
          .andExpect(MockMvcResultMatchers.content().contentType("text/html;charset=UTF-8"))
          .andReturn();

      // all values of the metadata base entity should be present in the view
      org.assertj.core.api.Assertions.assertThat(mvcResult.getResponse().getContentAsString())
          .contains(
              digitalObject.getId(),
              digitalObject.getBaseMetadata().getTitle(),
              digitalObject.getBaseMetadata().getDescription(),
              digitalObject.getBaseMetadata().getCreator(),
              digitalObject.getBaseMetadata().getRights(),
              digitalObject.getPublisher(),
              digitalObject.getObjectType(),
              digitalObject.getProject().getProjectAbbr(),
              digitalObject.getFunder()
          );


    }

    @Test
    public void getDigitalObjectContainsExpectedFunder() throws Exception {

      DigitalObject digitalObject = TestDigitalObject.generate();

      digitalObjectRepository.save(digitalObject);

      String url = String.format("/api/v1/projects/%s/objects/%s", testProject.getProjectAbbr(), digitalObject.getId());

      MvcResult mvcResult = mockMvc.perform(
              MockMvcRequestBuilders.get(url)
                  .accept(MediaType.TEXT_HTML)
                  .contentType(MediaType.TEXT_HTML)
          )
          .andExpect(status().isOk())
          .andExpect(MockMvcResultMatchers.view().name("DigitalObject/show"))
          .andExpect(MockMvcResultMatchers.content().contentType("text/html;charset=UTF-8"))
          .andReturn();

      // funder should be present in returned view
      org.assertj.core.api.Assertions.assertThat(mvcResult.getResponse().getContentAsString())
          .contains(
              digitalObject.getFunder()
          );


    }


  }

  @Test
  public void getObjectJsonReturnsDigitalObjectWhenItExists() throws Exception {

    DigitalObject digitalObject = TestDigitalObject.generate();
    digitalObjectRepository.save(digitalObject);

    MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/projects/{projectAbbr}/objects/{id}", testProject.getProjectAbbr(), digitalObject.getId())
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andReturn();

    Assertions.assertTrue(mvcResult.getResponse().getContentAsString().contains(digitalObject.getId()));

  }

  @Test
  public void getObjectJsonThrowsExceptionWhenDigitalObjectDoesNotExist() throws Exception {
    mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/projects/{projectAbbr}/objects/{id}", testProject.getProjectAbbr(), "nonExistentId")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isNotFound());
  }

  @Test
  public void getProjectObjectsJsonReturnsEmptyListWhenNoDigitalObjectsExistForProject() throws Exception {
    MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/projects/{projectAbbr}/objects", testProject.getProjectAbbr())
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andReturn();

    Assertions.assertEquals("[]", mvcResult.getResponse().getContentAsString());
  }

  @Test
  public void getProjectObjectsJsonReturnsDigitalObjectsWhenTheyExistForProject() throws Exception {
    final DigitalObject digitalObject = TestDigitalObject.generate();
    digitalObjectRepository.save(digitalObject);

    MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/projects/{projectAbbr}/objects", testProject.getProjectAbbr())
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andReturn();

    Assertions.assertTrue(mvcResult.getResponse().getContentAsString().contains(digitalObject.getId()));

  }

  @Test
  public void deleteObjectRemovesDigitalObjectWhenItExists() throws Exception {
    final DigitalObject digitalObject = TestDigitalObject.generate();
    digitalObjectRepository.save(digitalObject);

    mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/projects/{projectAbbr}/objects/{id}", testProject.getProjectAbbr(), digitalObject.getId())
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().is3xxRedirection());

    Assertions.assertFalse(digitalObjectRepository.existsById(digitalObject.getId()));
  }

  @Test
  public void deleteObjectDoesNotThrowExceptionWhenDigitalObjectDoesNotExist() throws Exception {
    mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/projects/{projectAbbr}/objects/{id}", testProject.getProjectAbbr(), "nonExistentId")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().is3xxRedirection());
  }

  @Test
  public void getFindAllIdsReturnsExpectedObjectIds() throws Exception {

    final DigitalObject digitalObject1 = TestDigitalObject.generate(testProject.getProjectAbbr(), testProject.getProjectAbbr() +  ".8d7");
    digitalObjectRepository.save(
        digitalObject1
    );

    final DigitalObject digitalObject2 = TestDigitalObject.generate(testProject.getProjectAbbr(), digitalObject1.getId() +  ".123");
    digitalObjectRepository.save(
        digitalObject2
    );

    MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/projects/{projectAbbr}/objects?style=idlist", testProject.getProjectAbbr())
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andReturn();

    org.assertj.core.api.Assertions.assertThat(mvcResult.getResponse().getContentAsString())
        .contains(digitalObject1.getId(), digitalObject2.getId());


  }

}