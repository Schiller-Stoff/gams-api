package org.ddh.gamsapi.domain.Datastream;

import org.assertj.core.api.Assertions;
import org.ddh.gamsapi.TestUtilities.TestUser;
import org.ddh.gamsapi.infrastructure.System.security.IUserPrincipalAuditorMapping;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.auditing.AuditingHandler;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.transaction.annotation.Transactional;
import org.ddh.gamsapi.domain.Datastream.utils.exceptions.DatastreamNotFoundException;
import org.ddh.gamsapi.domain.Datastream.utils.interfaces.IDatastreamContentRepository;
import org.ddh.gamsapi.domain.Datastream.utils.interfaces.IDatastreamRepository;
import org.ddh.gamsapi.domain.Datastream.utils.interfaces.IDatastreamService;
import org.ddh.gamsapi.domain.DigitalObject.DigitalObject;
import org.ddh.gamsapi.domain.DigitalObject.utils.interfaces.IDigitalObjectRepository;
import org.ddh.gamsapi.IntegrationTest;
import org.ddh.gamsapi.TestUtilities.TestDataBuilder;
import org.ddh.gamsapi.TestUtilities.TestDataSet;
import org.ddh.gamsapi.TestUtilities.TestDatastreamContent;

import java.util.Optional;
import java.util.Set;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc(addFilters = false) // deactivates security filters
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DatastreamControllerIT extends IntegrationTest {

  @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private IDigitalObjectRepository digitalObjectRepository;

  @Autowired
  private IDatastreamRepository datastreamRepository;

  @Autowired
  private IDatastreamService datastreamService;

  @Autowired
  private IDatastreamContentRepository datastreamContentRepository;

  /**
   * Classes need to mock authenticated users when changing datastreams
   */
  @MockitoBean
  private AuditingHandler auditingHandler;
  @MockitoBean
  private IUserPrincipalAuditorMapping userPrincipalAuditorMapping;

  @Autowired
  private TestDataBuilder testDataBuilder;

  private TestDataSet testDataSet;

  @BeforeEach
  void setup() {
    testDataSet = testDataBuilder.buildTestDataSet();
    // needed when changing digital objects
    Mockito.when(userPrincipalAuditorMapping.getCurrentAuditor())
        .thenReturn(Optional.of(TestUser.USERNAME.getValue()));
  }

  @Nested
  class WebClientTests {

    @Test
    @Transactional
    void getDatastreamRendersExpectedDsidInView() throws Exception {

      String url = String.format(
          "/api/curation/v1/projects/%s/objects/%s/datastreams/%s",
          testDataSet.project().getProjectAbbr(),
          testDataSet.digitalObject().getId(),
          testDataSet.mainDatastream().getDsid());

      MvcResult mvcResult = mockMvc.perform(
          MockMvcRequestBuilders.get(url)
              .contentType(MediaType.TEXT_HTML)
              .accept(MediaType.TEXT_HTML)
          )
          .andExpect(status().isOk())
          .andExpect(MockMvcResultMatchers.view().name("Datastream/show"))
          .andExpect(MockMvcResultMatchers.content().contentType("text/html;charset=UTF-8"))
          .andReturn();

      Assertions.assertThat(mvcResult.getResponse().getContentAsString())
          .contains(
              testDataSet.mainDatastream().getDsid(),
              testDataSet.digitalObject().getId(),
              testDataSet.project().getProjectAbbr()
          );

    }


    @Test
    @Transactional
    void datastreamViewDisplaysExpectedMetadata() throws Exception {

      String url = String.format(
          "/api/curation/v1/projects/%s/objects/%s/datastreams/%s",
          testDataSet.project().getProjectAbbr(),
          testDataSet.digitalObject().getId(),
          testDataSet.mainDatastream().getDsid()
      );

      MvcResult mvcResult = mockMvc.perform(
              MockMvcRequestBuilders.get(url)
                  .contentType(MediaType.TEXT_HTML)
                  .accept(MediaType.TEXT_HTML)
          )
          .andExpect(status().isOk())
          .andExpect(MockMvcResultMatchers.view().name("Datastream/show"))
          .andExpect(MockMvcResultMatchers.content().contentType("text/html;charset=UTF-8"))
          .andReturn();

      String responseContent = mvcResult.getResponse().getContentAsString();

      Assertions.assertThat(responseContent)
          .contains(
              testDataSet.mainDatastream().getDsid(),
              testDataSet.mainDatastream().getMimeType(),
              testDataSet.mainDatastream().getFilePath(),
              testDataSet.mainDatastream().getBaseMetadata().getTitle(),
              testDataSet.mainDatastream().getBaseMetadata().getDescription(),
              testDataSet.mainDatastream().getBaseMetadata().getCreator()
          );

      Assertions.assertThat(testDataSet.mainDatastream().getTags()).isNotEmpty();
      testDataSet.mainDatastream().getTags().forEach(tag -> Assertions.assertThat(responseContent).contains(tag));

      Assertions.assertThat(testDataSet.mainDatastream().getLang()).isNotEmpty();
      testDataSet.mainDatastream().getLang().forEach(lang -> Assertions.assertThat(responseContent).contains(lang));

    }

  }


  @Nested
  class DELETEDatastream {

    @Test
    void deleteDatastreamRemovesDatastreamFromDatabase() throws Exception {

      // DELETE request
      String url = String.format(
          "/api/curation/v1/projects/%s/objects/%s/datastreams/%s",
          testDataSet.project().getProjectAbbr(),
          testDataSet.digitalObject().getId(),
          testDataSet.mainDatastream().getDsid()
      );
      mockMvc.perform(
              MockMvcRequestBuilders.delete(url))
          .andExpect(status().is2xxSuccessful());

      // assertions
      var datastreamId = testDataSet.mainDatastream().deriveDatastreamId();
      org.junit.jupiter.api.Assertions.assertThrows(
          DatastreamNotFoundException.class,
          () -> datastreamService.findById(datastreamId)
      );

      Assertions.assertThat(datastreamService.findAll(testDataSet.digitalObject()))
          .isNotNull()
          .isEmpty();

    }

    @Test
    void deleteDatastreamViaFormRedirectsToObject() throws Exception {
      String url = String.format(
          "/api/curation/v1/projects/%s/objects/%s/datastreams/%s",
          testDataSet.project().getProjectAbbr(),
          testDataSet.digitalObject().getId(),
          testDataSet.mainDatastream().getDsid()
      );
      mockMvc.perform(
              MockMvcRequestBuilders.delete(url)
                  .contentType(MediaType.APPLICATION_FORM_URLENCODED))
          .andExpect(status().is3xxRedirection());

      var datastreamId = testDataSet.mainDatastream().deriveDatastreamId();
      org.junit.jupiter.api.Assertions.assertThrows(
          DatastreamNotFoundException.class,
          () -> datastreamService.findById(datastreamId)
      );
    }

    @Test
    void deleteDatastreamViaJsonReturns204() throws Exception {
      String url = String.format(
          "/api/curation/v1/projects/%s/objects/%s/datastreams/%s",
          testDataSet.project().getProjectAbbr(),
          testDataSet.digitalObject().getId(),
          testDataSet.mainDatastream().getDsid()
      );
      mockMvc.perform(
              MockMvcRequestBuilders.delete(url)
                  .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isNoContent());

      var datastreamId = testDataSet.mainDatastream().deriveDatastreamId();
      org.junit.jupiter.api.Assertions.assertThrows(
          DatastreamNotFoundException.class,
          () -> datastreamService.findById(datastreamId)
      );

      Assertions.assertThat(datastreamService.findAll(testDataSet.digitalObject()))
          .isNotNull()
          .isEmpty();
    }

    @Test
    void deleteNonExistentDatastreamReturns404() throws Exception {
      String url = String.format(
          "/api/curation/v1/projects/%s/objects/%s/datastreams/%s",
          testDataSet.project().getProjectAbbr(),
          testDataSet.digitalObject().getId(),
          "NON_EXISTENT_DSID"
      );
      mockMvc.perform(
              MockMvcRequestBuilders.delete(url)
                  .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isNotFound());
    }
  }

  @Nested
  class GETAllDatastreams {

    @Test
    void findAllContainsExpectedDatastream() throws Exception {

      String url = String.format(
          "/api/curation/v1/projects/%s/objects/%s/datastreams",
          testDataSet.project().getProjectAbbr(),
          testDataSet.digitalObject().getId()
      );

      // Act
      MvcResult mvcResult = mockMvc.perform(
              MockMvcRequestBuilders.get(url)
                  .contentType(MediaType.APPLICATION_JSON)
                  .accept(MediaType.APPLICATION_JSON)
          )
          .andExpect(status().isOk())
          .andReturn();

      // Assert
      Assertions.assertThat(mvcResult.getResponse().getContentAsString())
          .contains(
              testDataSet.mainDatastream().getDsid(),
              testDataSet.digitalObject().getId(),
              testDataSet.mainDatastream().getSize().toString(),
              testDataSet.mainDatastream().getFilePath()
          );
    }

    @Test
    void findAllDsidsContainsExpectedDsid() throws Exception {

      String url = String.format(
          "/api/curation/v1/projects/%s/objects/%s/datastreams/dsids",
          testDataSet.project().getProjectAbbr(),
          testDataSet.digitalObject().getId()
      );

      // Act
      MvcResult mvcResult = mockMvc.perform(
              MockMvcRequestBuilders.get(url)
                  .contentType(MediaType.APPLICATION_JSON)
                  .accept(MediaType.APPLICATION_JSON)
          )
          .andExpect(status().isOk())
          .andReturn();

      // Assert
      Assertions.assertThat(mvcResult.getResponse().getContentAsString())
          .contains(testDataSet.mainDatastream().getDsid());

    }

  }


  @Nested
  class GETDatastreamJSON {

    @Test
    void getDatastreamJsonContainsExpectedValues() throws Exception {

      String url = String.format(
          "/api/curation/v1/projects/%s/objects/%s/datastreams/%s",
          testDataSet.project().getProjectAbbr(),
          testDataSet.digitalObject().getId(),
          testDataSet.mainDatastream().getDsid()
      );

      // Act
      MvcResult mvcResult = mockMvc.perform(
              MockMvcRequestBuilders.get(url)
                  .contentType(MediaType.APPLICATION_JSON)
                  .accept(MediaType.APPLICATION_JSON)
          )
          .andExpect(status().isOk())
          .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
          .andReturn();

      // Assert
      Assertions.assertThat(mvcResult.getResponse().getContentAsString())
          .contains(
              testDataSet.mainDatastream().getDsid(),
              testDataSet.digitalObject().getId(),
              testDataSet.mainDatastream().getBaseMetadata().getTitle(),
              testDataSet.mainDatastream().getBaseMetadata().getDescription(),
              testDataSet.mainDatastream().getBaseMetadata().getCreator()
          );

    }


  }


  @Nested
  class GETDatastreamContent {


    @Test
    void getDatastreamContentReturnsExpectedDatastreamContent() throws Exception {

      String url = String.format(
          "/api/curation/v1/projects/%s/objects/%s/datastreams/%s/content",
          testDataSet.project().getProjectAbbr(),
          testDataSet.digitalObject().getId(),
          testDataSet.mainDatastream().getDsid()
      );

      // Act
      MvcResult mvcResult = mockMvc.perform(
          MockMvcRequestBuilders.get(url)
      )
        .andExpect(status().isOk())
        .andReturn();

      // Assert
      Assertions.assertThat(mvcResult.getResponse()).isNotNull();
      Assertions.assertThat(mvcResult.getResponse().getContentAsString()).isEqualTo(TestDatastreamContent.CONTENT.getValue());

    }

    @Test
    void getDatastreamContentReturnsExpectedUtf8EncodingInContentType() throws Exception {

      String url = String.format(
          "/api/curation/v1/projects/%s/objects/%s/datastreams/%s/content",
          testDataSet.project().getProjectAbbr(),
          testDataSet.digitalObject().getId(),
          testDataSet.mainDatastream().getDsid()
      );

      // Act
      String contentType = mockMvc.perform(
          MockMvcRequestBuilders.get(url)
      ).andReturn().getResponse().getContentType();

      String expectedContentType = testDataSet.mainDatastream().getMimeType() + ";charset=utf-8";
      Assertions.assertThat(contentType).isNotNull();
      Assertions.assertThat(contentType.toLowerCase())
          .isEqualTo(expectedContentType);

    }

  }


  /**
   * Tests for .../datastream/... endpoint
   * e.g. .../datastream?tag=...
   */
  @Nested
  class SingleDatastreamFiltering {

    @Nested
    class MainResource {
      @Test
      void returnsErrorIfNoMainResourceIsSetOnTheDigitalObject() throws Exception {

        DigitalObject testDigitalObject = testDataSet.mainDatastream().getDigitalObject();

        // remove main resource from digital object
        testDigitalObject.setMainResource("");
        digitalObjectRepository.save(testDigitalObject);

        String url = String.format(
            "/api/curation/v1/projects/%s/objects/%s/datastream",
            testDataSet.project().getProjectAbbr(),
            testDigitalObject.getId()
        );

        mockMvc.perform(
                MockMvcRequestBuilders.get(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().is4xxClientError());


      }

      @Test
      void returnsClientErrorIfMainResourceIsNotFound() throws Exception {

        // set as main resource on digital object
        testDataSet.digitalObject().setMainResource("nonExistingDsid");
        digitalObjectRepository.save(testDataSet.digitalObject());

        String url = String.format(
            "/api/curation/v1/projects/%s/objects/%s/datastream",
            testDataSet.project().getProjectAbbr(),
            testDataSet.digitalObject().getId()
        );

        mockMvc.perform(
                MockMvcRequestBuilders.get(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().isNotFound());

      }


      @Test
      void returnsExpectedMainDatastreamJSONByDefault() throws Exception {

        String url = String.format(
            "/api/curation/v1/projects/%s/objects/%s/datastream",
            testDataSet.project().getProjectAbbr(),
            testDataSet.digitalObject().getId()
        );

        MvcResult mvcResult = mockMvc.perform(
                MockMvcRequestBuilders.get(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().isOk())
            .andReturn();

        Assertions.assertThat(mvcResult.getResponse().getContentAsString())
            .contains(
                testDataSet.mainDatastream().getDsid(),
                testDataSet.digitalObject().getId(),
                testDataSet.mainDatastream().getSize().toString(),
                testDataSet.mainDatastream().getFilePath()
            );

      }

    }

    @Nested
    class TagFiltering {

      @Test
      void returnsExpectedSingularDatastreamsByTag() throws Exception {

        Datastream datastream2 = testDataBuilder.addRandomDatastream(testDataSet);

        // set tags for datastream2 to 0
        datastream2.setTags(Set.of());
        datastreamRepository.save(datastream2);

        String url = String.format(
            "/api/curation/v1/projects/%s/objects/%s/datastream?tag=%s",
            testDataSet.project().getProjectAbbr(),
            testDataSet.digitalObject().getId(),
            testDataSet.mainDatastream().getTags().iterator().next()
        );

        // Act
        MvcResult mvcResult = mockMvc.perform(
                MockMvcRequestBuilders.get(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().isOk())
            .andReturn();

        // Assert
        Assertions.assertThat(mvcResult.getResponse().getContentAsString())
            .contains(
                testDataSet.mainDatastream().getDsid(),
                testDataSet.digitalObject().getId(),
                testDataSet.mainDatastream().getSize().toString(),
                testDataSet.mainDatastream().getFilePath()
            )
            .doesNotContain(
                datastream2.getDsid()
            );
      }

      @Test
      void throwsIfNoSingularDatastreamWasMatched() throws Exception {

        // first datastream uses default test-tags
        Datastream datastream1 = testDataBuilder.addRandomDatastream(testDataSet);

        // second datastream also uses default test-tags
        testDataBuilder.addRandomDatastream(testDataSet);

        final String TAG_MATCHES_BOTH_DATASTREAMS = datastream1.getTags().iterator().next();

        String url = String.format(
            "/api/curation/v1/projects/%s/objects/%s/datastream?tag=%s",
            testDataSet.project().getProjectAbbr(),
            testDataSet.digitalObject().getId(),
            TAG_MATCHES_BOTH_DATASTREAMS
        );

        // Act
        mockMvc.perform(
                MockMvcRequestBuilders.get(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().isConflict());

      }

      @Test
      void throwsIfNoDatastreamWasMatched() throws Exception {

        final String NOT_DEFINED_TEST_TAG = "test-tag-not-defined";

        String url = String.format(
            "/api/curation/v1/projects/%s/objects/%s/datastream?tag=%s",
            testDataSet.project().getProjectAbbr(),
            testDataSet.digitalObject().getId(),
            NOT_DEFINED_TEST_TAG
        );

        // Act
        mockMvc.perform(
                MockMvcRequestBuilders.get(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().isNotFound());
      }


      @Test
      void filtersAreCombinedWithAndLogic() throws Exception {

        // create additional datastream a shared tag as the main datastream
        final String SHARED_TAG = testDataSet.mainDatastream().getTags().iterator().next();
        final String UNIQUE_TAG = "test-tag-unique";
        Datastream datastream2 = testDataBuilder.addRandomDatastream(testDataSet);
        datastream2.setTags(
            Set.of(
                SHARED_TAG,
                UNIQUE_TAG
            )
        );
        datastreamRepository.save(datastream2);

        String url = String.format(
            "/api/curation/v1/projects/%s/objects/%s/datastream?tag=%s&tag=%s",
            testDataSet.project().getProjectAbbr(),
            testDataSet.digitalObject().getId(),
            SHARED_TAG,
            UNIQUE_TAG
        );

        // Act
        var mvcResult = mockMvc.perform(
                MockMvcRequestBuilders.get(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().isOk())
            .andReturn();

        final String NOT_CONTAINED_DSID_VALUE = "\"dsid\":" + "\"" + testDataSet.mainDatastream().getDsid() + "\"";

        Assertions.assertThat(mvcResult.getResponse().getContentAsString())
            .contains(
                datastream2.getDsid(),
                testDataSet.digitalObject().getId(),
                UNIQUE_TAG
            ).doesNotContain(
                NOT_CONTAINED_DSID_VALUE
            );

      }


    }

    @Nested
    class DatastreamContent {

      @Test
      void returnsExpectedMainDatastreamContent() throws Exception {

        final String URL = String.format(
            "/api/curation/v1/projects/%s/objects/%s/datastream/content",
            testDataSet.project().getProjectAbbr(),
            testDataSet.digitalObject().getId()
        );

        // Act
        MvcResult mvcResult = mockMvc.perform(
                MockMvcRequestBuilders.get(URL)
            )
            .andExpect(status().isOk())
            .andReturn();

        // Assert
        Assertions.assertThat(mvcResult.getResponse())
            .isNotNull();
        Assertions.assertThat(mvcResult.getResponse().getContentAsString())
            .isEqualTo(TestDatastreamContent.CONTENT.getValue());

      }

      @Test
      void returnsErrorIfNoMainResourceWasSet() throws Exception {

        // make sure that mein resource is not set
        testDataSet.digitalObject().setMainResource(null);
        digitalObjectRepository.save(testDataSet.digitalObject());

        final String URL = String.format(
            "/api/curation/v1/projects/%s/objects/%s/datastream/content",
            testDataSet.project().getProjectAbbr(),
            testDataSet.digitalObject().getId()
        );

        // Act
        mockMvc.perform(
                MockMvcRequestBuilders.get(URL)
            )
            .andExpect(status().is4xxClientError());
      }

      @Test
      void allowsToAccessSingularDatastreamContentViaTagFiltering() throws Exception {

        final var TEST_DATASTREAM_CONTENT = "___DEMO_CONTENT___";

        final String SHARED_TAG = testDataSet.mainDatastream().getTags().iterator().next();
        final String UNIQUE_TAG = "test-tag-unique";

        Datastream datastream2 = testDataBuilder.addRandomDatastream(testDataSet);
        datastream2.setTags(Set.of(UNIQUE_TAG, SHARED_TAG));
        datastreamRepository.save(datastream2);

        // save content for datastream2
        datastreamContentRepository.save(
            TEST_DATASTREAM_CONTENT.getBytes(),
            datastream2.deriveDatastreamId()
        );

        // url
        final String URL = String.format(
            "/api/curation/v1/projects/%s/objects/%s/datastream/content?tag=%s&tag=%s",
            testDataSet.project().getProjectAbbr(),
            testDataSet.digitalObject().getId(),
            SHARED_TAG,
            UNIQUE_TAG
        );

        // Act
        MvcResult mvcResult = mockMvc.perform(
                MockMvcRequestBuilders.get(URL)
            )
            .andExpect(status().isOk())
            .andReturn();

        // Assert
        Assertions.assertThat(mvcResult.getResponse())
            .isNotNull();

        // should match datastream2 content
        Assertions.assertThat(mvcResult.getResponse().getContentAsString())
            .isEqualTo(TEST_DATASTREAM_CONTENT);

      }
    }



  }


  /**
   * Tests for .../datastreams/... endpoint
   */
  @Nested
  class MultipleDatastreamsFiltering {


    @Test
    void returnsAJSONListOfExpectedDatastreams() throws Exception {


      Datastream datastream2 = testDataBuilder.addRandomDatastream(testDataSet);

      String url = String.format(
          "/api/curation/v1/projects/%s/objects/%s/datastreams",
          testDataSet.project().getProjectAbbr(),
          testDataSet.digitalObject().getId()
      );

      // Act
      MvcResult mvcResult = mockMvc.perform(
              MockMvcRequestBuilders.get(url)
                  .contentType(MediaType.APPLICATION_JSON)
                  .accept(MediaType.APPLICATION_JSON)
          )
          .andExpect(status().isOk())
          .andReturn();


      // Assert
      Assertions.assertThat(mvcResult.getResponse().getContentAsString())
          .contains(
              testDataSet.mainDatastream().getDsid(),
              testDataSet.digitalObject().getId(),
              testDataSet.mainDatastream().getSize().toString(),
              testDataSet.mainDatastream().getFilePath()
          )
          .contains(
              datastream2.getDsid(),
              testDataSet.digitalObject().getId(),
              datastream2.getSize().toString(),
              datastream2.getFilePath()
          );

    }

    @Test
    void returnsAnEmptyListOfNoDatastreamsWereFound() throws Exception {

      // remove main datastream from test data set
      datastreamRepository.delete(testDataSet.mainDatastream());

      String url = String.format(
          "/api/curation/v1/projects/%s/objects/%s/datastreams",
          testDataSet.project().getProjectAbbr(),
          testDataSet.digitalObject().getId()
      );

      // Act
      MvcResult mvcResult = mockMvc.perform(
              MockMvcRequestBuilders.get(url)
                  .contentType(MediaType.APPLICATION_JSON)
                  .accept(MediaType.APPLICATION_JSON)
          )
          .andExpect(status().isOk())
          .andReturn();


      // Assert
      Assertions.assertThat(mvcResult.getResponse().getContentAsString())
          .isNotNull()
          .doesNotContain(
              testDataSet.digitalObject().getId(),
              "fileName",
              "baseMetadata"
          );

    }

    @Test
    void returnsExpectedDatastreamsIfMultipleTagsWereUsed() throws Exception {

      final String SHARED_TAG = testDataSet.mainDatastream().getTags().iterator().next();
      final String UNIQUE_TAG = "test-tag-unique";

      Datastream datastream2 = testDataBuilder.addRandomDatastream(testDataSet);
      datastream2.setTags(
          Set.of(
              UNIQUE_TAG,
              SHARED_TAG
          )
      );
      datastreamRepository.save(datastream2);

      // using both unique and shared tag -> should only return datastream2
      String url = String.format(
          "/api/curation/v1/projects/%s/objects/%s/datastreams?tag=%s&tag=%s&pageSize=100",
          testDataSet.project().getProjectAbbr(),
          testDataSet.digitalObject().getId(),
          SHARED_TAG,
          UNIQUE_TAG
      );

      // Act
      MvcResult mvcResult = mockMvc.perform(
              MockMvcRequestBuilders.get(url)
                  .contentType(MediaType.APPLICATION_JSON)
                  .accept(MediaType.APPLICATION_JSON)
          )
          .andExpect(status().isOk())
          .andReturn();

      final String EXPECTED_NOT_CONTAINED_DSID_VALUE = "\"dsid\":" + "\"" + testDataSet.mainDatastream().getDsid() + "\"";

      // Assert
      Assertions.assertThat(mvcResult.getResponse().getContentAsString())
          .doesNotContain(
              EXPECTED_NOT_CONTAINED_DSID_VALUE
          )
          .contains(
              datastream2.getDsid(),
              testDataSet.digitalObject().getId(),
              datastream2.getSize().toString(),
              datastream2.getFilePath()
          );

    }

  }

  @Nested
  class PUTDatastreams {

    @Test
    @WithMockUser
    void putReturns201WithCreatedDatastream() throws Exception {
      MockMultipartFile file = new MockMultipartFile(
          "file", "new_upload.txt", "text/plain", "test content".getBytes()
      );

      String url = String.format(
          "/api/curation/v1/projects/%s/objects/%s/datastreams/%s",
          testDataSet.project().getProjectAbbr(),
          testDataSet.digitalObject().getId(),
          "new_upload.txt"
      );

      // MockMvc: multipart defaults to POST, override to PUT
      mockMvc.perform(
              MockMvcRequestBuilders.multipart(url)
                  .file(file)
                  .param("title", "Test Title")
                  .param("creator", "Test Creator")
                  .param("rights", "CC BY 4.0")
                  .param("description", "Test Description")
                  .with(request -> { request.setMethod("PUT"); return request; })
                  .accept(MediaType.APPLICATION_JSON)
                  .with(SecurityMockMvcRequestPostProcessors.csrf())
          )
          .andExpect(status().isCreated())
          .andExpect(MockMvcResultMatchers.jsonPath("$.dsid").value("new_upload.txt"));
    }

    @Test
    @WithMockUser
    void putReturns409OnDuplicateDsid() throws Exception {
      MockMultipartFile file = new MockMultipartFile(
          "file",
          testDataSet.mainDatastream().getDsid(),
          "text/plain",
          "data".getBytes()
      );

      String url = String.format(
          "/api/curation/v1/projects/%s/objects/%s/datastreams/%s",
          testDataSet.project().getProjectAbbr(),
          testDataSet.digitalObject().getId(),
          testDataSet.mainDatastream().getDsid()
      );

      mockMvc.perform(
              MockMvcRequestBuilders.multipart(url)
                  .file(file)
                  .param("title", "Test")
                  .param("creator", "Creator")
                  .param("rights", "Rights")
                  .with(request -> { request.setMethod("PUT"); return request; })
                  .accept(MediaType.APPLICATION_JSON)
                  .with(SecurityMockMvcRequestPostProcessors.csrf())
          )
          .andExpect(status().isConflict());
    }

    @Test
    void putRequiresAuthentication() throws Exception {
      MockMultipartFile file = new MockMultipartFile(
          "file", "test.txt", "text/plain", "data".getBytes()
      );

      String url = String.format(
          "/api/curation/v1/projects/%s/objects/%s/datastreams/%s",
          testDataSet.project().getProjectAbbr(),
          testDataSet.digitalObject().getId(),
          "test.txt"
      );

      mockMvc.perform(
              MockMvcRequestBuilders.multipart(url)
                  .file(file)
                  .param("title", "Test")
                  .param("creator", "Creator")
                  .param("rights", "Rights")
                  .with(request -> { request.setMethod("PUT"); return request; })
                  .with(SecurityMockMvcRequestPostProcessors.csrf())
                  .with(SecurityMockMvcRequestPostProcessors.anonymous())
          )
          .andExpect(status().is4xxClientError());
    }

    // === Webclient (POST form) ===

    @Test
    @WithMockUser
    void formPostRedirectsAfterSuccessfulCreation() throws Exception {
      MockMultipartFile file = new MockMultipartFile(
          "file", "webclient_upload.txt", "text/plain", "data".getBytes()
      );

      String url = String.format(
          "/api/curation/v1/projects/%s/objects/%s/datastreams",
          testDataSet.project().getProjectAbbr(),
          testDataSet.digitalObject().getId()
      );

      mockMvc.perform(
              MockMvcRequestBuilders.multipart(url)
                  .file(file)
                  .param("dsid", "webclient_upload.txt")
                  .param("title", "Test Title")
                  .param("creator", "Test Creator")
                  .param("rights", "CC BY 4.0")
                  .param("description", "Test Description")
                  .accept(MediaType.TEXT_HTML)
                  .with(SecurityMockMvcRequestPostProcessors.csrf())
          )
          .andExpect(status().is3xxRedirection());

      // Verify the datastream was actually persisted
      DatastreamId dsId = new DatastreamId(
          "webclient_upload.txt",
          testDataSet.digitalObject().getId()
      );
      Assertions.assertThat(datastreamRepository.findById(dsId)).isPresent();
    }

    @Test
    @WithMockUser
    void formPostFailsOnDuplicate() throws Exception {
      MockMultipartFile file = new MockMultipartFile(
          "file",
          testDataSet.mainDatastream().getDsid(),
          "text/plain",
          "data".getBytes()
      );

      String url = String.format(
          "/api/curation/v1/projects/%s/objects/%s/datastreams",
          testDataSet.project().getProjectAbbr(),
          testDataSet.digitalObject().getId()
      );

      mockMvc.perform(
              MockMvcRequestBuilders.multipart(url)
                  .file(file)
                  .param("dsid", testDataSet.mainDatastream().getDsid())
                  .param("title", "Test")
                  .param("creator", "Creator")
                  .param("rights", "Rights")
                  .accept(MediaType.TEXT_HTML)
                  .with(SecurityMockMvcRequestPostProcessors.csrf())
          )
          .andExpect(status().is4xxClientError());
    }

    @Test
    @WithMockUser
    void formPostFailsOnValidationError() throws Exception {
      MockMultipartFile file = new MockMultipartFile(
          "file", "test.txt", "text/plain", "data".getBytes()
      );

      String url = String.format(
          "/api/curation/v1/projects/%s/objects/%s/datastreams",
          testDataSet.project().getProjectAbbr(),
          testDataSet.digitalObject().getId()
      );

      // Missing required 'title' field
      mockMvc.perform(
              MockMvcRequestBuilders.multipart(url)
                  .file(file)
                  .param("dsid", "test.txt")
                  .param("creator", "Creator")
                  .param("rights", "Rights")
                  .accept(MediaType.TEXT_HTML)
                  .with(SecurityMockMvcRequestPostProcessors.csrf())
          )
          .andExpect(status().is4xxClientError());
    }
  }


  @Nested
  class PatchDatastreamMetadata {

    @Test
    void updatesMetadataViaJsonPatch() throws Exception {
      String url = String.format(
          "/api/curation/v1/projects/%s/objects/%s/datastreams/%s",
          testDataSet.project().getProjectAbbr(),
          testDataSet.digitalObject().getId(),
          testDataSet.mainDatastream().getDsid()
      );

      String patchJson = """
          {
            "title": "Updated via PATCH",
            "description": "New description"
          }
          """;

      MvcResult mvcResult = mockMvc.perform(
              MockMvcRequestBuilders.patch(url)
                  .contentType(MediaType.APPLICATION_JSON)
                  .accept(MediaType.APPLICATION_JSON)
                  .content(patchJson)
          )
          .andExpect(status().isOk())
          .andReturn();

      Assertions.assertThat(mvcResult.getResponse().getContentAsString())
          .contains("Updated via PATCH")
          .contains("New description");
    }

    @Test
    void preservesUnchangedFieldsViaJsonPatch() throws Exception {
      String originalRights = testDataSet.mainDatastream().getBaseMetadata().getRights();

      String url = String.format(
          "/api/curation/v1/projects/%s/objects/%s/datastreams/%s",
          testDataSet.project().getProjectAbbr(),
          testDataSet.digitalObject().getId(),
          testDataSet.mainDatastream().getDsid()
      );

      String patchJson = """
          {
            "title": "Only title changes"
          }
          """;

      mockMvc.perform(
              MockMvcRequestBuilders.patch(url)
                  .contentType(MediaType.APPLICATION_JSON)
                  .accept(MediaType.APPLICATION_JSON)
                  .content(patchJson)
          )
          .andExpect(status().isOk());

      // verify via repository
      Datastream persisted = datastreamRepository.findById(
          testDataSet.mainDatastream().deriveDatastreamId()
      ).orElseThrow();
      Assertions.assertThat(persisted.getBaseMetadata().getRights())
          .isEqualTo(originalRights);
    }

    @Test
    void returns400ForEmptyTitle() throws Exception {
      String url = String.format(
          "/api/curation/v1/projects/%s/objects/%s/datastreams/%s",
          testDataSet.project().getProjectAbbr(),
          testDataSet.digitalObject().getId(),
          testDataSet.mainDatastream().getDsid()
      );

      String patchJson = """
          {
            "title": ""
          }
          """;

      mockMvc.perform(
              MockMvcRequestBuilders.patch(url)
                  .contentType(MediaType.APPLICATION_JSON)
                  .accept(MediaType.APPLICATION_JSON)
                  .content(patchJson)
          )
          .andExpect(status().isBadRequest());
    }

    @Test
    void returns404ForNonExistentDatastream() throws Exception {
      String url = String.format(
          "/api/curation/v1/projects/%s/objects/%s/datastreams/%s",
          testDataSet.project().getProjectAbbr(),
          testDataSet.digitalObject().getId(),
          "DOES_NOT_EXIST.txt"
      );

      String patchJson = """
          {
            "title": "irrelevant"
          }
          """;

      mockMvc.perform(
              MockMvcRequestBuilders.patch(url)
                  .contentType(MediaType.APPLICATION_JSON)
                  .accept(MediaType.APPLICATION_JSON)
                  .content(patchJson)
          )
          .andExpect(status().isNotFound());
    }

    @Test
    void updatesTags() throws Exception {
      // Re-fetch to safely check precondition on lazy collection
      Datastream fresh = datastreamRepository.findById(
          testDataSet.mainDatastream().deriveDatastreamId()
      ).orElseThrow();
      org.assertj.core.api.Assertions.assertThat(fresh.getTags()).isNotEmpty();

      String url = String.format(
          "/api/curation/v1/projects/%s/objects/%s/datastreams/%s",
          testDataSet.project().getProjectAbbr(),
          testDataSet.digitalObject().getId(),
          testDataSet.mainDatastream().getDsid()
      );

      String patchJson = """
          {
            "tags": ["patched-tag1", "patched-tag2"]
          }
          """;

      mockMvc.perform(
              MockMvcRequestBuilders.patch(url)
                  .contentType(MediaType.APPLICATION_JSON)
                  .accept(MediaType.APPLICATION_JSON)
                  .content(patchJson)
          )
          .andExpect(status().isOk());

      Datastream persisted = datastreamRepository.findById(
          testDataSet.mainDatastream().deriveDatastreamId()
      ).orElseThrow();
      Assertions.assertThat(persisted.getTags())
          .containsExactlyInAnyOrder("patched-tag1", "patched-tag2");
    }
  }


  @Nested
  class UpdateDatastreamContent {

    @Test
    void updatesContentViaFileUpload() throws Exception {
      String url = String.format(
          "/api/curation/v1/projects/%s/objects/%s/datastreams/%s/content",
          testDataSet.project().getProjectAbbr(),
          testDataSet.digitalObject().getId(),
          testDataSet.mainDatastream().getDsid()
      );

      MockMultipartFile newFile = new MockMultipartFile(
          "file", "updated.txt", "text/plain",
          "updated content via controller test".getBytes()
      );

      MvcResult mvcResult = mockMvc.perform(
              MockMvcRequestBuilders.multipart(url)
                  .file(newFile)
                  .with(request -> {
                    request.setMethod("POST");
                    return request;
                  })
                  .accept(MediaType.APPLICATION_JSON)
          )
          .andExpect(status().isOk())
          .andReturn();

      Assertions.assertThat(mvcResult.getResponse().getContentAsString())
          .contains(testDataSet.mainDatastream().getDsid());
    }

    @Test
    void returns404ForNonExistentDatastream() throws Exception {
      String url = String.format(
          "/api/curation/v1/projects/%s/objects/%s/datastreams/%s/content",
          testDataSet.project().getProjectAbbr(),
          testDataSet.digitalObject().getId(),
          "DOES_NOT_EXIST.txt"
      );

      MockMultipartFile newFile = new MockMultipartFile(
          "file", "test.txt", "text/plain", "content".getBytes()
      );

      mockMvc.perform(
              MockMvcRequestBuilders.multipart(url)
                  .file(newFile)
                  .with(request -> {
                    request.setMethod("POST");
                    return request;
                  })
                  .accept(MediaType.APPLICATION_JSON)
          )
          .andExpect(status().isNotFound());
    }
  }

}
