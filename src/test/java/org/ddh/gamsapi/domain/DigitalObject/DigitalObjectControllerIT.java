package org.ddh.gamsapi.domain.DigitalObject;

import org.ddh.gamsapi.IntegrationTest;
import org.ddh.gamsapi.TestUtilities.TestDataBuilder;
import org.ddh.gamsapi.TestUtilities.TestDataSet;
import org.ddh.gamsapi.domain.DigitalObject.utils.interfaces.IDigitalObjectRepository;
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
          digitalObjectRepository.findDigitalObjectById(
              testDataSet.digitalObject().getId())
          )
            .isNotPresent();

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

    @Nested
    public class DigitalObjectModification {


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
      public void headDigitalObjectReturnsLastModifiedDate() throws Exception {

        // Act
        mockMvc.perform(MockMvcRequestBuilders.head("/api/v1/projects/{projectAbbr}/objects/{id}",
                    testDataSet.project().getProjectAbbr(),
                    testDataSet.digitalObject().getId())
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(MockMvcResultMatchers.header().exists("Last-Modified"));
      }

      @Test
      public void headDigitalObjectReturnsExpectedLastModifiedDate() throws Exception {

        // expected date
        Date expectedLastModified = testDataSet.digitalObject().getModified();
        // remove milliseconds (the database works with milliseconds but the header does not - because of ISO RFC 1123)
        expectedLastModified.setTime(expectedLastModified.getTime() / 1000 * 1000);

        // Act
        String digitalObjectLastModified = mockMvc.perform(
            MockMvcRequestBuilders.head(
                "/api/v1/projects/{projectAbbr}/objects/{id}",
                    testDataSet.project().getProjectAbbr(),
                    testDataSet.digitalObject().getId()))
            .andReturn().getResponse().getHeader("Last-Modified");

        org.assertj.core.api.Assertions.assertThat(digitalObjectLastModified).isNotNull();

        // parse lastModified to Date
        DateTimeFormatter formatter = DateTimeFormatter.RFC_1123_DATE_TIME;
        ZonedDateTime zonedDateTime = ZonedDateTime.parse(digitalObjectLastModified, formatter);
        ZonedDateTime localZonedDateTime = zonedDateTime.withZoneSameInstant(ZoneId.systemDefault());
        Date projectLastModifiedHeaderValueAsDate = Date.from(localZonedDateTime.toInstant());

        org.assertj.core.api.Assertions.assertThat(projectLastModifiedHeaderValueAsDate)
            .isNotNull()
            .isEqualTo(expectedLastModified);

      }

    }


    @Nested
    public class SubResourcesModified {

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
                "/api/v1/projects/{projectAbbr}/objects/{id}/datastreams", testDataSet.project().getProjectAbbr(), testDataSet.digitalObject().getId()
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
                "/api/v1/projects/{projectAbbr}/objects/{id}/datastreams", testDataSet.project().getProjectAbbr(), testDataSet.digitalObject().getId()
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

        final String URL = String.format("/api/v1/projects/%s/objects/%s/datastreams", testDataSet.project().getProjectAbbr(), testDataSet.digitalObject().getId());

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

        final String URL = String.format("/api/v1/projects/%s/objects/%s/datastreams", testDataSet.digitalObject().getProject().getProjectAbbr(), testDataSet.digitalObject().getId());

        mockMvc.perform(
            MockMvcRequestBuilders
                .head(URL)
                .header("If-Modified-Since", ifModifiedSinceHeader)
        ).andExpect(
            status().isNotModified()
        );

      }

    }




  }

  @Nested
  public class GETRequests {

    @Nested
    public class GETAllDigitalObjects {

      String REQUEST_URL = "";

      @BeforeEach
      public void setup() {
          REQUEST_URL = String.format(
              "/api/v1/projects/%s/objects",
              testDataSet.project().getProjectAbbr()
          );
      }

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

      @Test
      public void tagFilterReturnsExpectedDigitalObject() throws Exception {
        final String TAG_FILTER_REQUEST_URL = String.format(
            "%s?tag=%s",
            REQUEST_URL,
            testDataSet.digitalObject().getTags().iterator().next()
        );

        // Act
        MvcResult mvcResult = mockMvc.perform(
                MockMvcRequestBuilders.get(TAG_FILTER_REQUEST_URL)
                    .contentType(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().isOk())
            .andReturn();

        // Assert
        String response = mvcResult.getResponse().getContentAsString();

        org.assertj.core.api.Assertions.assertThat(response)
            .contains(testDataSet.digitalObject().getId())
            .contains(testDataSet.digitalObject().getProject().getProjectAbbr());

        // assert that all tags are present in the response
        testDataSet.digitalObject().getTags().forEach(tag -> {
          org.assertj.core.api.Assertions.assertThat(response)
              .contains(tag);
        });
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

      @Test
      public void getDigitalObjectContainsExpectedChecksums(){
        org.assertj.core.api.Assertions.assertThat(digitalObjectJsonResponse)
            .contains(testDataSet.digitalObject().getBaseMetadata().getMd5Checksum())
            .contains(testDataSet.digitalObject().getBaseMetadata().getSha512Checksum());
      }

      @Test
      public void getDigitalObjectContainsExpectedTags(){
        for (String tag : testDataSet.digitalObject().getTags()) {
          org.assertj.core.api.Assertions.assertThat(digitalObjectJsonResponse)
              .contains(tag);
        }
      }

    }

    @Nested
    public class GETProjectTags {

      @Test
      public void getProjectTagsReturnsExpectedTags() throws Exception {

        String url = String.format("/api/v1/projects/%s/objects/tags", testDataSet.project().getProjectAbbr());

        MvcResult mvcResult = mockMvc.perform(
                MockMvcRequestBuilders.get(url)
                    .accept(MediaType.APPLICATION_JSON)
                    .contentType(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().isOk())
            .andReturn();

        String response = mvcResult.getResponse().getContentAsString();

        // contains digital object tags
        for (String tag : testDataSet.digitalObject().getTags()) {
          org.assertj.core.api.Assertions.assertThat(response)
              .contains(tag);
        }

      }


    }

  }

  @Nested
  public class WebclientTests {

    @Nested
    public class SingularObject {


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

        // contains digital object tags
        for (String tag : testDataSet.digitalObject().getTags()) {
          org.assertj.core.api.Assertions.assertThat(response)
              .contains(tag);
        }

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

      @Test
      public void getDigitalDigitalObjectContainsExpectedChecksums() throws Exception {
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

        // both checksums should be present in returned view
        org.assertj.core.api.Assertions.assertThat(mvcResult.getResponse().getContentAsString())
            .contains(
                testDataSet.digitalObject().getBaseMetadata().getMd5Checksum(),
                testDataSet.digitalObject().getBaseMetadata().getSha512Checksum()
            );
      }

    }


    @Nested
    public class DigitalObjectOverview {

      @Test
      public void getDigitalObjectsContainsExpectedTags() throws Exception {

        String url = String.format("/api/v1/projects/%s/objects", testDataSet.project().getProjectAbbr());

        MvcResult mvcResult = mockMvc.perform(
                MockMvcRequestBuilders.get(url)
                    .accept(MediaType.TEXT_HTML)
                    .contentType(MediaType.TEXT_HTML)
            )
            .andExpect(status().isOk())
            .andExpect(MockMvcResultMatchers.view().name("DigitalObject/show_all"))
            .andExpect(MockMvcResultMatchers.content().contentType("text/html;charset=UTF-8"))
            .andReturn();

        // contains digital object tags
        for (String tag : testDataSet.digitalObject().getTags()) {
          org.assertj.core.api.Assertions.assertThat(mvcResult.getResponse().getContentAsString())
              .contains(tag);
        }

      }

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