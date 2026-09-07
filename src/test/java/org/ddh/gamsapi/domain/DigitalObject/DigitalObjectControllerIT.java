package org.ddh.gamsapi.domain.DigitalObject;

import org.ddh.gamsapi.IntegrationTest;
import org.ddh.gamsapi.TestUtilities.TestDataBuilder;
import org.ddh.gamsapi.TestUtilities.TestDataSet;
import org.ddh.gamsapi.TestUtilities.TestDigitalObject;
import org.ddh.gamsapi.TestUtilities.TestUser;
import org.ddh.gamsapi.domain.DigitalObject.ArchivalRecord.IArchivalRecordRepository;
import org.ddh.gamsapi.domain.DigitalObject.utils.interfaces.IDigitalObjectRepository;
import org.ddh.gamsapi.infrastructure.System.security.IUserPrincipalAuditorMapping;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.auditing.AuditingHandler;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
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
import java.util.Set;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc(addFilters = false) // deactivates spring security for the test class
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DigitalObjectControllerIT extends IntegrationTest {

  @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private IDigitalObjectRepository digitalObjectRepository;

  @Autowired
  private IArchivalRecordRepository archivalRecordRepository;

  /**
   * Classes need to mock authenticated users when changing datastreams
   */
  @MockitoBean
  private AuditingHandler auditingHandler;
  @MockitoBean
  private IUserPrincipalAuditorMapping userPrincipalAuditorMapping;

  private TestDataSet testDataSet;

  @Autowired
  private TestDataBuilder testDataBuilder;

  @BeforeEach
  void setup() {
    testDataSet = testDataBuilder.buildTestDataSet();
    // needed when changing digital objects
    Mockito.when(userPrincipalAuditorMapping.getCurrentAuditor())
        .thenReturn(Optional.of(TestUser.USERNAME.getValue()));
  }

  @Nested
  class DELETERequests {

    @Test
    void mayNotDeleteObjectWhenArchivalRecordsExists() throws Exception {

      // Act
      mockMvc.perform(
              MockMvcRequestBuilders.delete("/api/curation/v1/projects/{projectAbbr}/objects/{id}", testDataSet.project().getProjectAbbr(), testDataSet.digitalObject().getId()
                  )
                  .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().is(409)); // status code for archival records exists

    }

    @Test
    void deleteDigitalObjectWhenItExists() throws Exception {

      archivalRecordRepository.delete(testDataSet.archivalRecord());

      // Act
      mockMvc.perform(
          MockMvcRequestBuilders.delete("/api/curation/v1/projects/{projectAbbr}/objects/{id}", testDataSet.project().getProjectAbbr(), testDataSet.digitalObject().getId()
              )
          .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk());

      // Assert
      org.assertj.core.api.Assertions.assertThat(
          digitalObjectRepository.findDigitalObjectById(
              testDataSet.digitalObject().getId())
          )
            .isNotPresent();

    }

    @Test
    void deleteObjectDoesShouldThrowExceptionWhenDigitalObjectDoesNotExist() throws Exception {
      mockMvc.perform(MockMvcRequestBuilders.delete(
                  "/api/curation/v1/projects/{projectAbbr}/objects/{id}",
                  testDataSet.project().getProjectAbbr(), "nonExistentId")
              .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().is4xxClientError());
    }
  }

  @Nested
  class HEADRequests {

    @Nested
    class DigitalObjectModification {


      @Test
      void headDigitalObjectReturns200ifObjectExists() throws Exception {
        // Act
        mockMvc.perform(MockMvcRequestBuilders.head("/api/curation/v1/projects/{projectAbbr}/objects/{id}",
                    testDataSet.project().getProjectAbbr(),
                    testDataSet.digitalObject().getId())
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
      }

      @Test
      void headDigitalObjectReturns404WhenObjectDoesNotExist() throws Exception {
        // Act
        mockMvc.perform(MockMvcRequestBuilders.head("/api/curation/v1/projects/{projectAbbr}/objects/{id}",
                    testDataSet.project().getProjectAbbr(),
                    "nonExistentId")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound());
      }

      @Test
      void headDigitalObjectReturnsLastModifiedDate() throws Exception {

        // Act
        mockMvc.perform(MockMvcRequestBuilders.head("/api/curation/v1/projects/{projectAbbr}/objects/{id}",
                    testDataSet.project().getProjectAbbr(),
                    testDataSet.digitalObject().getId())
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(MockMvcResultMatchers.header().exists("Last-Modified"));
      }

      @Test
      void headDigitalObjectReturnsExpectedLastModifiedDate() throws Exception {

        // expected: truncate to seconds since RFC 1123 has no sub-second precision
        Instant expectedLastModified = testDataSet.digitalObject().getModified()
            .truncatedTo(ChronoUnit.SECONDS);

        // Act
        String digitalObjectLastModified = mockMvc.perform(
                MockMvcRequestBuilders.head(
                    "/api/curation/v1/projects/{projectAbbr}/objects/{id}",
                    testDataSet.project().getProjectAbbr(),
                    testDataSet.digitalObject().getId()))
            .andReturn().getResponse().getHeader("Last-Modified");

        org.assertj.core.api.Assertions.assertThat(digitalObjectLastModified).isNotNull();

        // parse Last-Modified header (RFC 1123) to Instant
        Instant lastModifiedFromHeader = ZonedDateTime
            .parse(digitalObjectLastModified, DateTimeFormatter.RFC_1123_DATE_TIME)
            .toInstant();

        org.assertj.core.api.Assertions.assertThat(lastModifiedFromHeader)
            .isEqualTo(expectedLastModified);
      }

    }


    @Nested
    class SubResourcesModified {

      @Test
      void headDigitalObjectReturns200ifObjectExists() throws Exception {

        // Act
        mockMvc.perform(MockMvcRequestBuilders.head("/api/curation/v1/projects/{projectAbbr}/objects/{id}",
                    testDataSet.project().getProjectAbbr(),
                    testDataSet.digitalObject().getId())
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());

      }

      @Test
      void headDigitalObjectReturns404WhenObjectDoesNotExist() throws Exception {
        // Act
        mockMvc.perform(MockMvcRequestBuilders.head("/api/curation/v1/projects/{projectAbbr}/objects/{id}",
                    testDataSet.project().getProjectAbbr(),
                    "nonExistentId")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound());

      }

      @Test
      void HEADDigitalObjectResponsesWithIncludedLastModifiedHeader() throws Exception {

        // assert
        mockMvc.perform(
            MockMvcRequestBuilders.head(
                "/api/curation/v1/projects/{projectAbbr}/objects/{id}", testDataSet.project().getProjectAbbr(), testDataSet.digitalObject().getId()
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
      void HEADDigitalObjectResponsesWithExpectedLastModifiedHeaderDate() throws Exception {

        // Act
        String lastModifiedHeaderValue = mockMvc.perform(
            MockMvcRequestBuilders.head(
                    "/api/curation/v1/projects/{projectAbbr}/objects/{id}",
                    testDataSet.project().getProjectAbbr(),
                    testDataSet.digitalObject().getId()
                )
                .with(SecurityMockMvcRequestPostProcessors.csrf())
        ).andReturn().getResponse().getHeader("Last-Modified");

        // Assert
        org.assertj.core.api.Assertions.assertThat(lastModifiedHeaderValue).isNotNull();

        // parse Last-Modified header (RFC 1123) to Instant
        Instant lastModifiedFromHeader = ZonedDateTime
            .parse(lastModifiedHeaderValue, DateTimeFormatter.RFC_1123_DATE_TIME)
            .toInstant();

        // expected: truncate to seconds since RFC 1123 has no sub-second precision
        Instant expected = testDataSet.digitalObject().getModified()
            .truncatedTo(ChronoUnit.SECONDS);

        // assert
        org.assertj.core.api.Assertions.assertThat(lastModifiedFromHeader)
            .isEqualTo(expected);
      }

      /**
       * If a client supplies an If-Modified-Since header wit an invalid date format,
       * the server should respond with a 400 Bad Request status.
       * @throws Exception if the test fails (mockMvc.perform)
       */
      @Test
      void HEADProjectIfModifiedSinceIsMalformedRespondWith400() throws Exception {


        final String MALFORMED_DATE = "PETER";

        final String URL = String.format("/api/curation/v1/projects/%s/objects/%s", testDataSet.project().getProjectAbbr(), testDataSet.digitalObject().getId());

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
      void HEADProjectIfModifiedSinceRespondsWithIsNotModifiedHttpSTATUS() throws Exception {


        // Create a date in the future that's properly formatted for HTTP headers
        ZonedDateTime futureDate = ZonedDateTime.now(ZoneId.systemDefault()).plusYears(1);
        String ifModifiedSinceHeader = DateTimeFormatter.RFC_1123_DATE_TIME.format(futureDate);

        final String URL = String.format("/api/curation/v1/projects/%s/objects/%s", testDataSet.digitalObject().getProject().getProjectAbbr(), testDataSet.digitalObject().getId());

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
  class GETRequests {

    @Nested
    class GETAllDigitalObjects {

      String REQUEST_URL = "";

      @BeforeEach
      void setup() {
          REQUEST_URL = String.format(
              "/api/curation/v1/projects/%s/objects",
              testDataSet.project().getProjectAbbr()
          );
      }

      @Test
      void formatXmlReturnsExpectedDigitalObjectId() throws Exception {

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
      void trailingSlashWillReturnError() throws Exception {
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
      void tagFilterReturnsExpectedDigitalObject() throws Exception {
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
    class GETSingularDigitalObject {

      String digitalObjectJsonResponse;

      @BeforeEach
      void setup() throws Exception {
        String url = String.format(
            "/api/curation/v1/projects/%s/objects/%s",
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
      void getDigitalObjectContainsExpectedDublinCoreTestValue() {
        org.assertj.core.api.Assertions.assertThat(digitalObjectJsonResponse)
            .contains(testDataSet.dublinCoreEntry().getLanguage())
            .contains(testDataSet.dublinCoreEntry().getValue())
            .contains(testDataSet.digitalObject().getId())
            .contains(testDataSet.digitalObject().getProject().getProjectAbbr());
      }

      @Test
      void getAllObjectIdsReturnsExpectedIds() throws Exception {

        DigitalObject additionalDigitalObject = testDataBuilder.addRandomObject(testDataSet);

        final String URL = String.format("/api/curation/v1/projects/%s/objects/ids", testDataSet.project().getProjectAbbr());

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
      void getDigitalObjectContainsExpectedTags(){
        for (String tag : testDataSet.digitalObject().getTags()) {
          org.assertj.core.api.Assertions.assertThat(digitalObjectJsonResponse)
              .contains(tag);
        }
      }

    }

    @Nested
    class GETProjectTags {

      @Test
      void getProjectTagsReturnsExpectedTags() throws Exception {

        String url = String.format("/api/curation/v1/projects/%s/objects/tags", testDataSet.project().getProjectAbbr());

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
  class PATCHDigitalObject {

    @Nested
    class PatchDigitalObject {

      @Test
      void PATCHAllowsToUpdateTitle() throws Exception {
        final String url = String.format(
            "/api/curation/v1/projects/%s/objects/%s",
            testDataSet.project().getProjectAbbr(),
            testDataSet.digitalObject().getId()
        );

        final String NEW_TITLE = "Updated title";
        final String body = "{\"title\": \"" + NEW_TITLE + "\"}";

        String response = mockMvc.perform(
                MockMvcRequestBuilders.patch(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body)
            ).andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        org.assertj.core.api.Assertions.assertThat(response).contains(NEW_TITLE);

        // Verify via repository
        DigitalObject updated = digitalObjectRepository.findById(
            testDataSet.digitalObject().getId()
        ).orElseThrow();
        org.assertj.core.api.Assertions. assertThat(
                updated.getBaseMetadata().getTitle())
            .isEqualTo(NEW_TITLE);
      }

      @Test
      void PATCHPreservesUnchangedFields() throws Exception {
        final String url = String.format(
            "/api/curation/v1/projects/%s/objects/%s",
            testDataSet.project().getProjectAbbr(),
            testDataSet.digitalObject().getId()
        );

        String originalRights = testDataSet.digitalObject().getBaseMetadata().getRights();
        String originalPublisher = testDataSet.digitalObject().getPublisher();

        // Only update description
        final String body = "{\"description\": \"new desc\"}";

        mockMvc.perform(
            MockMvcRequestBuilders.patch(url)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        ).andExpect(status().isOk());

        DigitalObject updated = digitalObjectRepository.findById(
            testDataSet.digitalObject().getId()
        ).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(updated.getBaseMetadata().getDescription()).isEqualTo("new desc");
        org.assertj.core.api.Assertions. assertThat(updated.getBaseMetadata().getRights()).isEqualTo(originalRights);
        org.assertj.core.api.Assertions.assertThat(updated.getPublisher()).isEqualTo(originalPublisher);
      }

      @Test
      void PATCHRejectsEmptyTitle() throws Exception {
        final String url = String.format(
            "/api/curation/v1/projects/%s/objects/%s",
            testDataSet.project().getProjectAbbr(),
            testDataSet.digitalObject().getId()
        );

        final String body = "{\"title\": \"\"}";

        mockMvc.perform(
            MockMvcRequestBuilders.patch(url)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        ).andExpect(status().isBadRequest());
      }

      @Test
      void PATCHRequiresRequestBody() throws Exception {
        final String url = String.format(
            "/api/curation/v1/projects/%s/objects/%s",
            testDataSet.project().getProjectAbbr(),
            testDataSet.digitalObject().getId()
        );

        mockMvc.perform(
            MockMvcRequestBuilders.patch(url)
        ).andExpect(status().is4xxClientError());
      }

      @Test
      void PATCHReturns404ForNonExistentObject() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders.patch("/api/curation/v1/projects/test/objects/test.nonexistent")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\": \"test\"}")
        ).andExpect(status().isNotFound());
      }

      @Test
      void PATCHAllowsToUpdateMultipleFields() throws Exception {
        final String url = String.format(
            "/api/curation/v1/projects/%s/objects/%s",
            testDataSet.project().getProjectAbbr(),
            testDataSet.digitalObject().getId()
        );

        final String body = """
            {
                "title": "New Title",
                "description": "New Description",
                "funder": "New Funder"
            }
            """;

        mockMvc.perform(
            MockMvcRequestBuilders.patch(url)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        ).andExpect(status().isOk());

        DigitalObject updated = digitalObjectRepository.findById(
            testDataSet.digitalObject().getId()
        ).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(updated.getBaseMetadata().getTitle()).isEqualTo("New Title");
        org.assertj.core.api.Assertions.assertThat(updated.getBaseMetadata().getDescription()).isEqualTo("New Description");
        org.assertj.core.api.Assertions.assertThat(updated.getFunder()).isEqualTo("New Funder");
      }

      @Test
      void PATCHUpdatesModificationTimestamp() throws Exception {
        final String url = String.format(
            "/api/curation/v1/projects/%s/objects/%s",
            testDataSet.project().getProjectAbbr(),
            testDataSet.digitalObject().getId()
        );

        Instant beforeUpdate = Instant.now();
        Thread.sleep(50); // ensure timestamp difference

        mockMvc.perform(
            MockMvcRequestBuilders.patch(url)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\": \"Timestamp Test\"}")
        ).andExpect(status().isOk());

        DigitalObject updated = digitalObjectRepository.findById(
            testDataSet.digitalObject().getId()
        ).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(updated.getModified()).isAfter(beforeUpdate);
      }

      @Test
      void setsMainResourceViaJson() throws Exception {
        final String url = String.format(
            "/api/curation/v1/projects/%s/objects/%s",
            testDataSet.project().getProjectAbbr(),
            testDataSet.digitalObject().getId()
        );

        String body = String.format(
            "{\"mainResource\": \"%s\"}",
            testDataSet.mainDatastream().getDsid()
        );

        mockMvc.perform(
            MockMvcRequestBuilders.patch(url)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        ).andExpect(status().isOk());

        DigitalObject updated = digitalObjectRepository.findById(
            testDataSet.digitalObject().getId()
        ).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(updated.getMainResource())
            .isEqualTo(testDataSet.mainDatastream().getDsid());
      }

      @Test
      void rejectsInvalidDsid() throws Exception {
        final String url = String.format(
            "/api/curation/v1/projects/%s/objects/%s",
            testDataSet.project().getProjectAbbr(),
            testDataSet.digitalObject().getId()
        );

        mockMvc.perform(
            MockMvcRequestBuilders.patch(url)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"mainResource\": \"NONEXISTENT_DSID\"}")
        ).andExpect(status().isBadRequest());
      }

      @Test
      void clearsMainResourceViaEmptyString() throws Exception {
        // Pre-set mainResource
        DigitalObject obj = digitalObjectRepository.findById(
            testDataSet.digitalObject().getId()
        ).orElseThrow();
        obj.setMainResource(testDataSet.mainDatastream().getDsid());
        digitalObjectRepository.save(obj);

        final String url = String.format(
            "/api/curation/v1/projects/%s/objects/%s",
            testDataSet.project().getProjectAbbr(),
            testDataSet.digitalObject().getId()
        );

        mockMvc.perform(
            MockMvcRequestBuilders.patch(url)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"mainResource\": \"\"}")
        ).andExpect(status().isOk());

        DigitalObject updated = digitalObjectRepository.findById(
            testDataSet.digitalObject().getId()
        ).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(updated.getMainResource()).isNull();
      }

    }

    @Nested
    class PatchDigitalObjectFromForm {

      private String buildUrl() {
        return String.format(
            "/api/curation/v1/projects/%s/objects/%s",
            testDataSet.project().getProjectAbbr(),
            testDataSet.digitalObject().getId()
        );
      }

      private String buildRedirectUrl() {
        return "/api/curation/v1/projects/" + testDataSet.project().getProjectAbbr()
            + "/objects/" + testDataSet.digitalObject().getId();
      }

      @Test
      void updatesTitleAndRedirects() throws Exception {
        final String NEW_TITLE = "Updated via form";

        mockMvc.perform(
                MockMvcRequestBuilders.patch(buildUrl())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .param("title", NEW_TITLE)
            )
            .andExpect(status().is3xxRedirection())
            .andExpect(MockMvcResultMatchers.redirectedUrl(buildRedirectUrl()));

        DigitalObject updated = digitalObjectRepository.findById(
            testDataSet.digitalObject().getId()
        ).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(updated.getBaseMetadata().getTitle()).isEqualTo(NEW_TITLE);
      }

      @Test
      void updatesMultipleFieldsSimultaneously() throws Exception {
        final String NEW_TITLE = "Form Title";
        final String NEW_DESCRIPTION = "Form Description";
        final String NEW_FUNDER = "Form Funder";
        final String NEW_RIGHTS = "CC BY 4.0";

        mockMvc.perform(
                MockMvcRequestBuilders.patch(buildUrl())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .param("title", NEW_TITLE)
                    .param("description", NEW_DESCRIPTION)
                    .param("funder", NEW_FUNDER)
                    .param("rights", NEW_RIGHTS)
            )
            .andExpect(status().is3xxRedirection());

        DigitalObject updated = digitalObjectRepository.findById(
            testDataSet.digitalObject().getId()
        ).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(updated.getBaseMetadata().getTitle()).isEqualTo(NEW_TITLE);
        org.assertj.core.api.Assertions.assertThat(updated.getBaseMetadata().getDescription()).isEqualTo(NEW_DESCRIPTION);
        org.assertj.core.api.Assertions.assertThat(updated.getBaseMetadata().getRights()).isEqualTo(NEW_RIGHTS);
        org.assertj.core.api.Assertions.assertThat(updated.getFunder()).isEqualTo(NEW_FUNDER);
      }

      @Test
      void preservesUnchangedFields() throws Exception {
        String originalRights = testDataSet.digitalObject().getBaseMetadata().getRights();
        String originalCreator = testDataSet.digitalObject().getBaseMetadata().getCreator();
        String originalPublisher = testDataSet.digitalObject().getPublisher();

        mockMvc.perform(
                MockMvcRequestBuilders.patch(buildUrl())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .param("title", "Only title changes")
            )
            .andExpect(status().is3xxRedirection());

        DigitalObject updated = digitalObjectRepository.findById(
            testDataSet.digitalObject().getId()
        ).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(updated.getBaseMetadata().getTitle()).isEqualTo("Only title changes");
        org.assertj.core.api.Assertions.assertThat(updated.getBaseMetadata().getRights()).isEqualTo(originalRights);
        org.assertj.core.api.Assertions.assertThat(updated.getBaseMetadata().getCreator()).isEqualTo(originalCreator);
        org.assertj.core.api.Assertions.assertThat(updated.getPublisher()).isEqualTo(originalPublisher);
      }

      @Test
      void parsesCommaSeparatedTagsCorrectly() throws Exception {
        mockMvc.perform(
                MockMvcRequestBuilders.patch(buildUrl())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .param("tagsCommaSeparated", "alpha, beta, gamma")
                    .param("tagsPresent", "true")
            )
            .andExpect(status().is3xxRedirection());

        DigitalObject updated = digitalObjectRepository.findById(
            testDataSet.digitalObject().getId()
        ).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(updated.getTags())
            .containsExactlyInAnyOrder("alpha", "beta", "gamma");
      }

      @Test
      void handlesWhitespaceAndEmptyEntriesInTags() throws Exception {
        mockMvc.perform(
                MockMvcRequestBuilders.patch(buildUrl())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .param("tagsCommaSeparated", " tag1 ,  tag2 ,, , tag3 ")
                    .param("tagsPresent", "true")
            )
            .andExpect(status().is3xxRedirection());

        DigitalObject updated = digitalObjectRepository.findById(
            testDataSet.digitalObject().getId()
        ).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(updated.getTags())
            .containsExactlyInAnyOrder("tag1", "tag2", "tag3");
      }

      @Test
      void removesAllTagsWhenInputIsEmpty() throws Exception {
        // Precondition: object has tags
        org.assertj.core.api.Assertions.assertThat(testDataSet.digitalObject().getTags()).isNotEmpty();

        mockMvc.perform(
                MockMvcRequestBuilders.patch(buildUrl())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .param("tagsCommaSeparated", "")
                    .param("tagsPresent", "true")
            )
            .andExpect(status().is3xxRedirection());

        DigitalObject updated = digitalObjectRepository.findById(
            testDataSet.digitalObject().getId()
        ).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(updated.getTags()).isEmpty();
      }

      @Test
      void tagsUnchangedWhenTagsNotSubmitted() throws Exception {
        Set<String> originalTags = testDataSet.digitalObject().getTags();

        // No tagsCommaSeparated and no tagsPresent param
        mockMvc.perform(
                MockMvcRequestBuilders.patch(buildUrl())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .param("title", "Tag preservation test")
            )
            .andExpect(status().is3xxRedirection());

        DigitalObject updated = digitalObjectRepository.findById(
            testDataSet.digitalObject().getId()
        ).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(updated.getTags())
            .containsExactlyInAnyOrderElementsOf(originalTags);
      }

      @Test
      void rejectsEmptyRequiredFields() throws Exception {
        mockMvc.perform(
                MockMvcRequestBuilders.patch(buildUrl())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .param("title", "")
            )
            .andExpect(status().isBadRequest());
      }

      @Test
      void returns404ForNonExistentObject() throws Exception {
        String url = String.format(
            "/api/curation/v1/projects/%s/objects/%s.nonexistent",
            testDataSet.project().getProjectAbbr(),
            testDataSet.project().getProjectAbbr()
        );

        mockMvc.perform(
                MockMvcRequestBuilders.patch(url)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .param("title", "irrelevant")
            )
            .andExpect(status().isNotFound());
      }

      @Test
      void updatesModificationTimestamp() throws Exception {
        Instant beforeUpdate = Instant.now();
        Thread.sleep(50);

        mockMvc.perform(
                MockMvcRequestBuilders.patch(buildUrl())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .param("title", "Timestamp form test")
            )
            .andExpect(status().is3xxRedirection());

        DigitalObject updated = digitalObjectRepository.findById(
            testDataSet.digitalObject().getId()
        ).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(updated.getModified()).isAfter(beforeUpdate);
      }

      @Test
      void setsMainResourceViaForm() throws Exception {
        final String url = String.format(
            "/api/curation/v1/projects/%s/objects/%s",
            testDataSet.project().getProjectAbbr(),
            testDataSet.digitalObject().getId()
        );

        mockMvc.perform(
                MockMvcRequestBuilders.patch(url)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .param("mainResource", testDataSet.mainDatastream().getDsid())
            )
            .andExpect(status().is3xxRedirection());

        DigitalObject updated = digitalObjectRepository.findById(
            testDataSet.digitalObject().getId()
        ).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(updated.getMainResource())
            .isEqualTo(testDataSet.mainDatastream().getDsid());
      }
    }

  }

  @Nested
  class WebclientTests {

    @Nested
    class SingularObject {


      @Test
      void getDigitalObjectRendersExpectedViewValues() throws Exception {


        String url = String.format("/api/curation/v1/projects/%s/objects/%s", testDataSet.project().getProjectAbbr(), testDataSet.digitalObject().getId());

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
      void digitalObjectShowsExpectedDatastreamDsids() throws Exception {

        var additionalDatastream = testDataBuilder.addRandomDatastream(testDataSet);

        String url = String.format("/api/curation/v1/projects/%s/objects/%s", testDataSet.project().getProjectAbbr(), testDataSet.digitalObject().getId());

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
      void getDigitalObjectRendersExpectedBaseMetadata() throws Exception {


        String url = String.format("/api/curation/v1/projects/%s/objects/%s", testDataSet.project().getProjectAbbr(), testDataSet.digitalObject().getId());

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
      void getDigitalObjectContainsExpectedFunder() throws Exception {

        String url = String.format("/api/curation/v1/projects/%s/objects/%s", testDataSet.project().getProjectAbbr(), testDataSet.digitalObject().getId());

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

      @Nested
      class CreateObjectFromForm {

        @Test
        void createsExpectedObject() throws Exception {

          final String ID_SUFFIX = "demo123";
          final String TEST_OBJECT_ID = testDataSet.project().getProjectAbbr() + "." + ID_SUFFIX;
          final String URL = "/api/curation/v1/projects/" + testDataSet.project().getProjectAbbr() + "/objects";

          mockMvc.perform(
              MockMvcRequestBuilders.post(URL)
                  .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                  .param("idSuffix", ID_SUFFIX)
                  .param("title", TestDigitalObject.DIGITAL_OBJECT_TITLE.getValue())
                  .param("creator", TestDigitalObject.DIGITAL_OBJECT_CREATOR.getValue())
                  .param("rights", TestDigitalObject.DIGITAL_OBJECT_RIGHTS.getValue())
                  .param("publisher", TestDigitalObject.DIGITAL_OBJECT_PUBLISHER.getValue())
                  .param("description", TestDigitalObject.DIGITAL_OBJECT_DESCRIPTION.getValue())
          )
              .andExpect(status().is3xxRedirection());

          // Verify object was persisted
          var created = digitalObjectRepository.findById(TEST_OBJECT_ID);
          org.assertj.core.api.Assertions.assertThat(created).isPresent();

          var digitalObject = created.get();
          org.assertj.core.api.Assertions.assertThat(digitalObject.getId()).isEqualTo(TEST_OBJECT_ID);
          org.assertj.core.api.Assertions.assertThat(digitalObject.getBaseMetadata().getTitle()).isEqualTo(TestDigitalObject.DIGITAL_OBJECT_TITLE.getValue());
          org.assertj.core.api.Assertions.assertThat(digitalObject.getBaseMetadata().getCreator()).isEqualTo(TestDigitalObject.DIGITAL_OBJECT_CREATOR.getValue());
          org.assertj.core.api.Assertions.assertThat(digitalObject.getBaseMetadata().getRights()).isEqualTo(TestDigitalObject.DIGITAL_OBJECT_RIGHTS.getValue());
          org.assertj.core.api.Assertions.assertThat(digitalObject.getPublisher()).isEqualTo(TestDigitalObject.DIGITAL_OBJECT_PUBLISHER.getValue());
          org.assertj.core.api.Assertions.assertThat(digitalObject.getBaseMetadata().getDescription()).isEqualTo(TestDigitalObject.DIGITAL_OBJECT_DESCRIPTION.getValue());
          org.assertj.core.api.Assertions.assertThat(digitalObject.getProject().getProjectAbbr())
              .isEqualTo(testDataSet.project().getProjectAbbr());

        }

      }

    }

    @Nested
    class DigitalObjectOverview {

      @Test
      void getDigitalObjectsContainsExpectedTags() throws Exception {

        String url = String.format("/api/curation/v1/projects/%s/objects", testDataSet.project().getProjectAbbr());

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
  void getObjectJsonReturnsDigitalObjectWhenItExists() throws Exception {

    MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders.get("/api/curation/v1/projects/{projectAbbr}/objects/{id}", testDataSet.project().getProjectAbbr(), testDataSet.digitalObject().getId())
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andReturn();

    Assertions.assertTrue(mvcResult.getResponse().getContentAsString().contains(testDataSet.digitalObject().getId()));

  }

  @Test
  void getObjectJsonThrowsExceptionWhenDigitalObjectDoesNotExist() throws Exception {
    mockMvc.perform(MockMvcRequestBuilders.get("/api/curation/v1/projects/{projectAbbr}/objects/{id}", testDataSet.project().getProjectAbbr(), "nonExistentId")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isNotFound());
  }

  @Test
  void getProjectObjectsJsonReturnsEmptyListWhenNoDigitalObjectsExistForProject() throws Exception {
    testDataBuilder.removeAllExceptProjects(testDataSet);
    MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders.get("/api/curation/v1/projects/{projectAbbr}/objects", testDataSet.project().getProjectAbbr())
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andReturn();

    org.assertj.core.api.Assertions.assertThat(
        mvcResult.getResponse().getContentAsString()
    ).contains("\"results\":[]");

  }

  @Test
  void getProjectObjectsJsonReturnsDigitalObjectsWhenTheyExistForProject() throws Exception {
    MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders.get("/api/curation/v1/projects/{projectAbbr}/objects", testDataSet.project().getProjectAbbr())
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andReturn();

    Assertions.assertTrue(mvcResult.getResponse().getContentAsString().contains(testDataSet.digitalObject().getId()));

  }


  @Test
  void getFindAllIdsReturnsExpectedObjectIds() throws Exception {

    final DigitalObject additionalDigitalObject = testDataBuilder.addRandomObject(testDataSet);

    MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders.get("/api/curation/v1/projects/{projectAbbr}/objects?style=idlist", testDataSet.project().getProjectAbbr())
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andReturn();

    org.assertj.core.api.Assertions.assertThat(mvcResult.getResponse().getContentAsString())
        .contains(testDataSet.digitalObject().getId(), additionalDigitalObject.getId());


  }

}