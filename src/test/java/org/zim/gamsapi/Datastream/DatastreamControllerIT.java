package org.zim.gamsapi.Datastream;

import org.assertj.core.api.Assertions;
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
import org.springframework.transaction.annotation.Transactional;
import org.zim.gamsapi.Datastream.exceptions.DatastreamNotFoundException;
import org.zim.gamsapi.Datastream.interfaces.IDatastreamContentRepository;
import org.zim.gamsapi.Datastream.interfaces.IDatastreamRepository;
import org.zim.gamsapi.Datastream.interfaces.IDatastreamService;
import org.zim.gamsapi.DigitalObject.DigitalObject;
import org.zim.gamsapi.DigitalObject.IDigitalObjectRepository;
import org.zim.gamsapi.IntegrationTest;
import org.zim.gamsapi.enums.TestDataBuilder;
import org.zim.gamsapi.enums.TestDataSet;
import org.zim.gamsapi.enums.TestDatastreamContent;

import java.util.Set;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc(addFilters = false) // deactivates security filters
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class DatastreamControllerIT extends IntegrationTest {

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

  @MockitoBean
  private AuditingHandler auditingHandler;

  @Autowired
  private TestDataBuilder testDataBuilder;

  private TestDataSet testDataSet;

  @BeforeEach
  public void setup() {
    testDataSet = testDataBuilder.buildTestDataSet();
  }

  @Nested
  public class WebClientTests {

    @Test
    @Transactional
    public void getDatastreamRendersExpectedDsidInView() throws Exception {

      String url = String.format(
          "/api/v1/projects/%s/objects/%s/datastreams/%s",
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
    public void datastreamViewDisplaysExpectedMetadata() throws Exception {

      String url = String.format(
          "/api/v1/projects/%s/objects/%s/datastreams/%s",
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
              testDataSet.mainDatastream().getFileName(),
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
  @Disabled("The current version of the REST-API does not support the deletion of individual datastreams. Only digital objects might be deleted.")
  public class DELETEDatastream {

    @Test
    public void deleteDatastreamRemovesDatastreamFromDatabase() throws Exception {

      // DELETE request
      String url = String.format(
          "/api/v1/projects/%s/objects/%s/datastreams/%s",
          testDataSet.project().getProjectAbbr(),
          testDataSet.digitalObject().getId(),
          testDataSet.mainDatastream().getDsid()
      );
      mockMvc.perform(
          MockMvcRequestBuilders.delete(url))
          .andExpect(status().is3xxRedirection());

      // assertions
      org.junit.jupiter.api.Assertions.assertThrows(
            DatastreamNotFoundException.class,
            () -> datastreamService.findById(testDataSet.mainDatastream().deriveDatastreamId()
          )
      );

      Assertions.assertThat(datastreamService.findAll(testDataSet.digitalObject()))
          .isNotNull()
          .isEmpty();

    }
  }

  @Nested
  public class GETAllDatastreams {

    @Test
    public void findAllContainsExpectedDatastream() throws Exception {

      String url = String.format(
          "/api/v1/projects/%s/objects/%s/datastreams",
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
              testDataSet.mainDatastream().getFileName()
          );
    }

    @Test
    public void findAllDsidsContainsExpectedDsid() throws Exception {

      String url = String.format(
          "/api/v1/projects/%s/objects/%s/datastreams/dsids",
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
  public class GETDatastreamJSON {

    @Test
    public void getDatastreamJsonContainsExpectedValues() throws Exception {

      String url = String.format(
          "/api/v1/projects/%s/objects/%s/datastreams/%s",
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
  public class GETDatastreamContent {


    @Test
    public void getDatastreamContentReturnsExpectedDatastreamContent() throws Exception {

      String url = String.format(
          "/api/v1/projects/%s/objects/%s/datastreams/%s/content",
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

  }


  /**
   * Tests for .../datastream/... endpoint
   * e.g. .../datastream?tag=...
   */
  @Nested
  public class SingleDatastreamFiltering {

    @Nested
    public class MainResource {
      @Test
      public void returnsErrorIfNoMainResourceIsSetOnTheDigitalObject() throws Exception {

        DigitalObject testDigitalObject = testDataSet.mainDatastream().getDigitalObject();

        // remove main resource from digital object
        testDigitalObject.setMainResource("");
        digitalObjectRepository.save(testDigitalObject);

        String url = String.format(
            "/api/v1/projects/%s/objects/%s/datastream",
            testDataSet.project().getProjectAbbr(),
            testDigitalObject.getId()
        );

        mockMvc.perform(
                MockMvcRequestBuilders.get(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().is5xxServerError());


      }

      @Test
      public void returnsClientErrorIfMainResourceIsNotFound() throws Exception {

        // set as main resource on digital object
        testDataSet.digitalObject().setMainResource("nonExistingDsid");
        digitalObjectRepository.save(testDataSet.digitalObject());

        String url = String.format(
            "/api/v1/projects/%s/objects/%s/datastream",
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
      public void returnsExpectedMainDatastreamJSONByDefault() throws Exception {

        String url = String.format(
            "/api/v1/projects/%s/objects/%s/datastream",
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
                testDataSet.mainDatastream().getFileName()
            );

      }

    }

    @Nested
    public class TagFiltering {

      @Test
      public void returnsExpectedSingularDatastreamsByTag() throws Exception {

        Datastream datastream2 = testDataBuilder.addRandomDatastream(testDataSet);

        // set tags for datastream2 to 0
        datastream2.setTags(Set.of());
        datastreamRepository.save(datastream2);

        String url = String.format(
            "/api/v1/projects/%s/objects/%s/datastream?tag=%s",
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
                testDataSet.mainDatastream().getFileName()
            )
            .doesNotContain(
                datastream2.getDsid()
            );
      }

      @Test
      public void throwsIfNoSingularDatastreamWasMatched() throws Exception {

        // first datastream uses default test-tags
        Datastream datastream1 = testDataBuilder.addRandomDatastream(testDataSet);

        // second datastream also uses default test-tags
        testDataBuilder.addRandomDatastream(testDataSet);

        final String TAG_MATCHES_BOTH_DATASTREAMS = datastream1.getTags().iterator().next();

        String url = String.format(
            "/api/v1/projects/%s/objects/%s/datastream?tag=%s",
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
      public void throwsIfNoDatastreamWasMatched() throws Exception {

        final String NOT_DEFINED_TEST_TAG = "test-tag-not-defined";

        String url = String.format(
            "/api/v1/projects/%s/objects/%s/datastream?tag=%s",
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
      public void filtersAreCombinedWithAndLogic() throws Exception {

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
            "/api/v1/projects/%s/objects/%s/datastream?tag=%s&tag=%s",
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
    public class DatastreamContent {

      @Test
      public void returnsExpectedMainDatastreamContent() throws Exception {

        final String URL = String.format(
            "/api/v1/projects/%s/objects/%s/datastream/content",
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
      public void returnsErrorIfNoMainResourceWasSet() throws Exception {

        // make sure that mein resource is not set
        testDataSet.digitalObject().setMainResource(null);
        digitalObjectRepository.save(testDataSet.digitalObject());

        final String URL = String.format(
            "/api/v1/projects/%s/objects/%s/datastream/content",
            testDataSet.project().getProjectAbbr(),
            testDataSet.digitalObject().getId()
        );

        // Act
        mockMvc.perform(
                MockMvcRequestBuilders.get(URL)
            )
            .andExpect(status().is5xxServerError());
      }

      @Test
      public void allowsToAccessSingularDatastreamContentViaTagFiltering() throws Exception {

        var TEST_DATASTREAM_CONTENT = "___DEMO_CONTENT___";

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
            "/api/v1/projects/%s/objects/%s/datastream/content?tag=%s&tag=%s",
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
  public class MultipleDatastreamsFiltering {


    @Test
    public void returnsAJSONListOfExpectedDatastreams() throws Exception {


      Datastream datastream2 = testDataBuilder.addRandomDatastream(testDataSet);

      String url = String.format(
          "/api/v1/projects/%s/objects/%s/datastreams",
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
              testDataSet.mainDatastream().getFileName()
          )
          .contains(
              datastream2.getDsid(),
              testDataSet.digitalObject().getId(),
              datastream2.getSize().toString(),
              datastream2.getFileName()
          );

    }

    @Test
    public void returnsAnEmptyListOfNoDatastreamsWereFound() throws Exception {

      // remove main datastream from test data set
      datastreamRepository.delete(testDataSet.mainDatastream());

      String url = String.format(
          "/api/v1/projects/%s/objects/%s/datastreams",
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
    public void returnsExpectedDatastreamsIfMultipleTagsWereUsed() throws Exception {

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
          "/api/v1/projects/%s/objects/%s/datastreams?tag=%s&tag=%s&pageSize=100",
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
              datastream2.getFileName()
          );

    }

  }

}
