package org.zim.gamsapi.DigitalObject;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.data.auditing.AuditingHandler;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.zim.gamsapi.GAMSCollection.GAMSCollection;
import org.zim.gamsapi.GAMSCollection.IGAMSCollectionRepository;
import org.zim.gamsapi.IntegrationTest;
import org.zim.gamsapi.enums.TestDataBuilder;
import org.zim.gamsapi.enums.TestDataSet;
import org.zim.gamsapi.enums.TestDigitalObject;
import org.zim.gamsapi.enums.TestGAMSCollection;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc(addFilters = false) // deactivates spring security for the test class
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class DigitalObjectControllerIT extends IntegrationTest {

  @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private IDigitalObjectRepository digitalObjectRepository;

  @Autowired
  private IGAMSCollectionRepository collectionRepository;

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
  public class DELETERequests {

    @Test
    public void deleteDigitalObjectWhenItExists() throws Exception {

      // Act
      mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/projects/{projectAbbr}/objects/{id}", testDataSet.project().getProjectAbbr(), testDataSet.digitalObject().getId())
          .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().is3xxRedirection());

      // Assert
      org.assertj.core.api.Assertions.assertThat(
          digitalObjectRepository.findDigitalObjectById(testDataSet.digitalObject().getId()))
            .isNotPresent();

    }

    @Test
    public void mayDeleteADigitalObjectReferencedByAGamsCollection() throws Exception {

      // Arrange
      DigitalObject digitalObject = TestDigitalObject.generate();
      digitalObjectRepository.save(digitalObject);

      // test collection references the test object automatically
      GAMSCollection gamsCollection = TestGAMSCollection.generate();
      collectionRepository.save(gamsCollection);

      // Act
      mockMvc.perform(
          MockMvcRequestBuilders.delete(
              "/api/v1/projects/{projectAbbr}/objects/{id}",
                  testDataSet.project().getProjectAbbr(),
                  digitalObject.getId())
              .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().is3xxRedirection());

    }

    @Test
    public void deleteObjectDoesShouldThrowExceptionWhenDigitalObjectDoesNotExist() throws Exception {
      mockMvc.perform(MockMvcRequestBuilders.delete(
                  "/api/v1/projects/{projectAbbr}/objects/{id}",
                  testDataSet.project().getProjectAbbr(), "nonExistentId")
              .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().is4xxClientError());
    }
  }

  @Nested
  public class HEADRequests {

    @Test
    public void headDigitalObjectReturns200ifObjectExists() throws Exception {

      // Act
      mockMvc.perform(MockMvcRequestBuilders.head("/api/v1/projects/{projectAbbr}/objects/{id}",
                  testDataSet.project().getProjectAbbr(),
                  testDataSet.digitalObject().getId())
          .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk());

    }

    @Test
    public void headDigitalObjectReturns404WhenObjectDoesNotExist() throws Exception {
      // Act
      mockMvc.perform(MockMvcRequestBuilders.head("/api/v1/projects/{projectAbbr}/objects/{id}",
                  testDataSet.project().getProjectAbbr(),
                  "nonExistentId")
          .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isNotFound());

    }

    @Test
    public void HEADDigitalObjectResponsesWithIncludedLastModifiedHeader() throws Exception {

      // assert
      mockMvc.perform(
          MockMvcRequestBuilders.head(
              "/api/v1/projects/{projectAbbr}/objects/{id}", testDataSet.project().getProjectAbbr(), testDataSet.digitalObject().getId()
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

      // Act
      String lastModifiedHeaderValue = mockMvc.perform(
          MockMvcRequestBuilders.head(
              "/api/v1/projects/{projectAbbr}/objects/{id}", testDataSet.project().getProjectAbbr(), testDataSet.digitalObject().getId()
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
      Date expectedDate = testDataSet.digitalObject().getModified();
      // remove milliseconds (the database works with milliseconds but the header does not - because of ISO RFC 1123)
      expectedDate.setTime(expectedDate.getTime() / 1000 * 1000);

      // assert same time
      org.assertj.core.api.Assertions.assertThat(lastModifiedHeaderValueAsDate).hasSameTimeAs(expectedDate);

    }

    /**
     * If a client supplies an If-Modified-Since header wit an invalid date format,
     * the server should respond with a 400 Bad Request status.
     * @throws Exception if the test fails (mockMvc.perform)
     */
    @Test
    public void HEADProjectIfModifiedSinceIsMalformedRespondWith400() throws Exception {


      final String MALFORMED_DATE = "PETER";

      final String URL = String.format("/api/v1/projects/%s/objects/%s", testDataSet.project().getProjectAbbr(), testDataSet.digitalObject().getId());

      mockMvc.perform(
          MockMvcRequestBuilders
              .head(URL)
              .header("If-Modified-Since", MALFORMED_DATE)
      ).andExpect(
          status().isBadRequest()
      );

    }

    /**
     * If a client supplies an If-Modified-Since header with a date that is after the last modified date of the object,
     * the server should respond with a 304 Not Modified status.
     * @throws Exception if the test fails (mockMvc.perform)
     */
    @Test
    public void HEADProjectIfModifiedSinceRespondsWithIsNotModifiedHttpSTATUS() throws Exception {


      // Create a date in the future that's properly formatted for HTTP headers
      ZonedDateTime futureDate = ZonedDateTime.now(ZoneId.systemDefault()).plusYears(1);
      String ifModifiedSinceHeader = DateTimeFormatter.RFC_1123_DATE_TIME.format(futureDate);

      final String URL = String.format("/api/v1/projects/%s/objects/%s", testDataSet.digitalObject().getProject().getProjectAbbr(), testDataSet.digitalObject().getId());

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
  public class GETRequests {

    @Nested
    public class GETAllDigitalObjects {

      final String REQUEST_URL = String.format(
          "/api/v1/projects/%s/objects",
          testDataSet.project().getProjectAbbr()
      );

      @Test
      public void formatXmlReturnsExpectedDigitalObjectId() throws Exception {

        final String FORMAT_XML_REQUEST_URL = String.format(
            "%s?format=xml",
            REQUEST_URL
        );

        // Act
        MvcResult mvcResult = mockMvc.perform(
                MockMvcRequestBuilders.get(FORMAT_XML_REQUEST_URL)
            )
            .andExpect(status().isOk())
            .andExpect(result -> result
                .getResponse()
                .getContentType()
                .equals(MediaType.APPLICATION_XML_VALUE))
            .andReturn();

        // Assert
        String response = mvcResult.getResponse().getContentAsString();

        org.assertj.core.api.Assertions.assertThat(response)
            .contains("<")
            .contains(">")
            .contains(testDataSet.digitalObject().getId())
            .contains(testDataSet.digitalObject().getProject().getProjectAbbr());
      }

      @Test
      public void trailingSlashWillReturnError() throws Exception {
        final String TRAILING_SLASH_REQUEST_URL = String.format(
            "%s/",
            REQUEST_URL
        );
        // Act & Assert
        mockMvc.perform(
                MockMvcRequestBuilders.get(TRAILING_SLASH_REQUEST_URL)
            )
            .andExpect(status().isNotFound());
      }

    }

    @Nested
    public class GETSingularDigitalObject {

      String digitalObjectJsonResponse;

      @BeforeEach
      public void setup() throws Exception {
        String url = String.format(
            "/api/v1/projects/%s/objects/%s",
            testDataSet.digitalObject().getProject().getProjectAbbr(),
            testDataSet.digitalObject().getId()
        );
        MvcResult mvcResult = mockMvc.perform(
                MockMvcRequestBuilders.get(url)
                    .accept(MediaType.APPLICATION_JSON)
                    .contentType(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().isOk())
            .andReturn();

        digitalObjectJsonResponse = mvcResult.getResponse().getContentAsString();
      }

      @Test
      public void getDigitalObjectContainsExpectedDublinCoreTestValue() {
        org.assertj.core.api.Assertions.assertThat(digitalObjectJsonResponse)
            .contains(testDataSet.dublinCoreEntry().getLanguage())
            .contains(testDataSet.dublinCoreEntry().getValue())
            .contains(testDataSet.digitalObject().getId())
            .contains(testDataSet.digitalObject().getProject().getProjectAbbr());
      }

      @Test
      public void getAllObjectIdsReturnsExpectedIds() throws Exception {

        DigitalObject additionalDigitalObject = testDataBuilder.addRandomObject(testDataSet);

        final String URL = String.format("/api/v1/projects/%s/objects/ids", testDataSet.project().getProjectAbbr());

        // Act
        MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders.get(URL)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andReturn();

        // Assert
        org.assertj.core.api.Assertions.assertThat(mvcResult.getResponse().getContentAsString())
            .contains(testDataSet.digitalObject().getId(), additionalDigitalObject.getId());
      }

    }


  }

  @Nested
  public class WebclientTests {


    @Test
    public void getDigitalObjectRendersExpectedViewValues() throws Exception {


      String url = String.format("/api/v1/projects/%s/objects/%s", testDataSet.project().getProjectAbbr(), testDataSet.digitalObject().getId());

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
              testDataSet.digitalObject().getId(),
              testDataSet.project().getProjectAbbr(),
              testDataSet.digitalObject().getObjectType(),
              testDataSet.digitalObject().getFunder(),
              testDataSet.digitalObject().getPublisher()
          );

    }


    @Test
    public void digitalObjectShowsExpectedDatastreamDsids() throws Exception {

      var additionalDatastream = testDataBuilder.addRandomDatastream(testDataSet);

      String url = String.format("/api/v1/projects/%s/objects/%s", testDataSet.project().getProjectAbbr(), testDataSet.digitalObject().getId());

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
              testDataSet.mainDatastream().getDsid(),
              additionalDatastream.getDsid()
          );

    }

    @Test
    public void getDigitalObjectRendersExpectedBaseMetadata() throws Exception {


      String url = String.format("/api/v1/projects/%s/objects/%s", testDataSet.project().getProjectAbbr(), testDataSet.digitalObject().getId());

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
              testDataSet.digitalObject().getId(),
              testDataSet.digitalObject().getBaseMetadata().getTitle(),
              testDataSet.digitalObject().getBaseMetadata().getDescription(),
              testDataSet.digitalObject().getBaseMetadata().getCreator(),
              testDataSet.digitalObject().getBaseMetadata().getRights(),
              testDataSet.digitalObject().getPublisher(),
              testDataSet.digitalObject().getObjectType(),
              testDataSet.digitalObject().getProject().getProjectAbbr(),
              testDataSet.digitalObject().getFunder()
          );


    }

    @Test
    public void getDigitalObjectContainsExpectedFunder() throws Exception {

      String url = String.format("/api/v1/projects/%s/objects/%s", testDataSet.project().getProjectAbbr(), testDataSet.digitalObject().getId());

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
              testDataSet.digitalObject().getFunder()
          );


    }


  }

  @Test
  public void getObjectJsonReturnsDigitalObjectWhenItExists() throws Exception {

    MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/projects/{projectAbbr}/objects/{id}", testDataSet.project().getProjectAbbr(), testDataSet.digitalObject().getId())
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andReturn();

    Assertions.assertTrue(mvcResult.getResponse().getContentAsString().contains(testDataSet.digitalObject().getId()));

  }

  @Test
  public void getObjectJsonThrowsExceptionWhenDigitalObjectDoesNotExist() throws Exception {
    mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/projects/{projectAbbr}/objects/{id}", testDataSet.project().getProjectAbbr(), "nonExistentId")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isNotFound());
  }

  @Test
  public void getProjectObjectsJsonReturnsEmptyListWhenNoDigitalObjectsExistForProject() throws Exception {
    testDataBuilder.removeAllExceptProjects(testDataSet);
    MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/projects/{projectAbbr}/objects", testDataSet.project().getProjectAbbr())
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andReturn();

    org.assertj.core.api.Assertions.assertThat(
        mvcResult.getResponse().getContentAsString()
    ).contains("\"results\":[]");

  }

  @Test
  public void getProjectObjectsJsonReturnsDigitalObjectsWhenTheyExistForProject() throws Exception {
    MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/projects/{projectAbbr}/objects", testDataSet.project().getProjectAbbr())
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andReturn();

    Assertions.assertTrue(mvcResult.getResponse().getContentAsString().contains(testDataSet.digitalObject().getId()));

  }


  @Test
  public void getFindAllIdsReturnsExpectedObjectIds() throws Exception {

    final DigitalObject additionalDigitalObject = testDataBuilder.addRandomObject(testDataSet);

    MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/projects/{projectAbbr}/objects?style=idlist", testDataSet.project().getProjectAbbr())
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andReturn();

    org.assertj.core.api.Assertions.assertThat(mvcResult.getResponse().getContentAsString())
        .contains(testDataSet.digitalObject().getId(), additionalDigitalObject.getId());


  }

}