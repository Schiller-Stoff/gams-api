package org.zim.gamsapi.Ingest;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.data.auditing.AuditingHandler;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockPart;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.transaction.annotation.Transactional;
import org.zim.gamsapi.Datastream.DatastreamId;
import org.zim.gamsapi.Datastream.interfaces.IDatastreamRepository;
import org.zim.gamsapi.Datastream.interfaces.IDatastreamContentRepository;
import org.zim.gamsapi.DigitalObject.IDigitalObjectRepository;
import org.zim.gamsapi.Ingest.utils.IngestStatics;
import org.zim.gamsapi.Ingest.utils.ZipUtils;
import org.zim.gamsapi.IntegrationTest;
import org.zim.gamsapi.Project.interfaces.IProjectRepository;
import org.zim.gamsapi.enums.TestBag;
import org.zim.gamsapi.enums.TestDatastream;
import org.zim.gamsapi.enums.TestDigitalObject;
import org.zim.gamsapi.enums.TestProject;

import java.io.File;
import java.io.IOException;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc(addFilters = false) // deactivates security filters
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class IngestControllerIT extends IntegrationTest {

  @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
  @Autowired
  private MockMvc mockMvc;

  @Autowired
  IProjectRepository projectRepository;

  @Autowired
  IDatastreamRepository datastreamRepository;

  @Autowired
  IDigitalObjectRepository digitalObjectRepository;

  @Autowired
  IDatastreamContentRepository datastreamContentRepository;

  // disables auditing
  @MockitoBean
  private AuditingHandler auditingHandler;


  File bagFile;

  @BeforeEach
  public void setup() throws IOException {
    bagFile = TestBag.loadFile();
    projectRepository.save(TestProject.generate());
  }

  @Test
  public void ingestCreatesAtLeastOneObjectAndOneDatastream() throws Exception {
    byte[] zippedBag = ZipUtils.zipDir(bagFile);
    MockPart mockPart = new MockPart(IngestStatics.FORM_PART_NAME.name, "test.zip", zippedBag);
    mockMvc
        .perform(
            multipart("/api/v1/projects/{projectAbbr}/objects", TestProject.PROJECT_ABBR.getValue())
            .part(mockPart)
        )
        .andExpect(status().isOk());


    Assertions.assertThat(datastreamRepository.findAll()).isNotEmpty();
    Assertions.assertThat(digitalObjectRepository.findAll()).isNotEmpty();

  }

  @Test
  public void ingestFailsIfProjectAbbrDiffersFromBagitSipJSONProject() throws Exception {
    final String MISMATCHING_PROJECT_ABBR = "different";
    // need to ensure that the different project is there (otherwise a 404 error will be thrown)
    projectRepository.save(TestProject.generate(MISMATCHING_PROJECT_ABBR));

    byte[] zippedBag = ZipUtils.zipDir(bagFile);
    MockPart mockPart = new MockPart(IngestStatics.FORM_PART_NAME.name, "test.zip", zippedBag);
    mockMvc
        .perform(
            multipart("/api/v1/projects/{projectAbbr}/objects", MISMATCHING_PROJECT_ABBR)
            .part(mockPart)
        )
        .andExpect(status().isBadRequest());
  }

  @Nested
  public class IngestDigitalObjectGETAssertions {

    /**
     * Performing test data ingest before each test.
     * @throws Exception If the test fails.
     */
    @BeforeEach
    public void setUp() throws Exception {
      byte[] zippedBag = ZipUtils.zipDir(bagFile);
      MockPart mockPart = new MockPart(IngestStatics.FORM_PART_NAME.name, "test.zip", zippedBag);
      mockMvc
          .perform(
              multipart("/api/v1/projects/{projectAbbr}/objects", TestProject.PROJECT_ABBR.getValue())
                  .part(mockPart)
          )
          .andExpect(status().isOk());

    }



    @Nested
    public class WebViewAssertion{

      private String response;

      /**
       * Ingests a bag and retrieves the view of the test digital object.
       * @throws Exception If the test fails.
       */
      @BeforeEach
      public void setup() throws Exception {
        // check if the test response is already there
        if(response != null){
          if (response.length() > 10){
            return;
          }
          String msg = String.format("Response was not null but too short. Got %s", response);
          throw new IllegalStateException(msg);
        }

        final String URL = String.format("/api/v1/projects/%s/objects/%s", TestProject.PROJECT_ABBR.getValue(), TestDigitalObject.DIGITAL_OBJECT_ID.getValue());

        MvcResult mvcResult = mockMvc.perform(
                MockMvcRequestBuilders.get(URL)
                    .accept(MediaType.TEXT_HTML)
                    .contentType(MediaType.TEXT_HTML)
            )
            .andExpect(status().isOk())
            .andExpect(MockMvcResultMatchers.view().name("DigitalObject/show"))
            .andExpect(MockMvcResultMatchers.content().contentType("text/html;charset=UTF-8"))
            .andReturn();

        response = mvcResult.getResponse().getContentAsString();

      }

      @Test
      public void testDigitalObjectViewContainsExpectedObjectId(){
        Assertions.assertThat(response).contains(TestDigitalObject.DIGITAL_OBJECT_ID.getValue());
      }

      @Test
      public void digitalObjectViewShouldContainExpectedProjectAbbr(){
        Assertions.assertThat(response).contains(TestProject.PROJECT_ABBR.getValue());
      }

      @Test
      public void digitalObjectViewContainsExpectedTitle(){
        Assertions.assertThat(response).contains(TestDigitalObject.DIGITAL_OBJECT_TITLE.getValue());
      }

      @Test
      public void digitalObjectViewShouldContainExpectedDescription(){
        Assertions.assertThat(response).contains(TestDigitalObject.DIGITAL_OBJECT_DESCRIPTION.getValue());
      }

      @Test
      public void digitalObjectViewContainsExpectedCreator(){
        Assertions.assertThat(response).contains(TestDigitalObject.DIGITAL_OBJECT_CREATOR.getValue());
      }

      @Test
      public void digitalObjectViewContainsExpectedFunder(){
        Assertions.assertThat(response).contains(TestDigitalObject.DIGITAL_OBJECT_FUNDER.getValue());
      }

      @Test
      public void digitalObjectViewContainsExpectedPublisher(){
        Assertions.assertThat(response).contains(TestDigitalObject.DIGITAL_OBJECT_PUBLISHER.getValue());
      }

      @Test
      public void digitalObjectViewContainsExpectedRights(){
        Assertions.assertThat(response).contains(TestDigitalObject.DIGITAL_OBJECT_RIGHTS.getValue());
      }

      @Test
      public void digitalObjectViewContainsExpectedMainResourceTwice(){
        // check if label of main resource is there
        Assertions.assertThat(response).contains("main resource");
        // check if the value of the main resource is there
        Assertions.assertThat(response).contains(String.format("<p>%s</p>", TestDigitalObject.DIGITAL_OBJECT_MAIN_RESOURCE.getValue()));
        //match expected datastream id two times (once in list overview / once as main-resource)
        Assertions.assertThat(response).containsPattern(
            String.format("(%s.*?){2}", TestDigitalObject.DIGITAL_OBJECT_MAIN_RESOURCE.getValue())
        );

      }

    }






  }

  /**
   * Tests the view of the test-datastream after ingesting a bag.
   */
  @Nested
  public class IngestDatastreamWebViewAssertions {

    private String response;

    /**
     * Ingests a bag and retrieves the view of the test digital object.
     * @throws Exception If the test fails.
     */
    @BeforeEach
    public void setup() throws Exception {

      // check if the test response is already there
      if(response != null){
        if (response.length() > 10){
          return;
        }
        String msg = String.format("Response was not null but too short. Got %s", response);
        throw new IllegalStateException(msg);
      }


      byte[] zippedBag = ZipUtils.zipDir(bagFile);
      MockPart mockPart = new MockPart(IngestStatics.FORM_PART_NAME.name, "test.zip", zippedBag);
      mockMvc
          .perform(
              multipart("/api/v1/projects/{projectAbbr}/objects", TestProject.PROJECT_ABBR.getValue())
                  .part(mockPart)
          )
          .andExpect(status().isOk());

      final String URL = String.format("/api/v1/projects/%s/objects/%s/datastreams/%s", TestProject.PROJECT_ABBR.getValue(), TestDigitalObject.DIGITAL_OBJECT_ID.getValue(), TestDatastream.DSID.getValue());

      MvcResult mvcResult = mockMvc.perform(
              MockMvcRequestBuilders.get(URL)
                  .accept(MediaType.TEXT_HTML)
                  .contentType(MediaType.TEXT_HTML)
          )
          .andExpect(status().isOk())
          .andExpect(MockMvcResultMatchers.view().name("Datastream/show"))
          .andExpect(MockMvcResultMatchers.content().contentType("text/html;charset=UTF-8"))
          .andReturn();

      response = mvcResult.getResponse().getContentAsString();

    }


    @Test
    @Transactional
    public void datastreamViewContainsExpectedDatastreamId(){
      Assertions.assertThat(response).contains(TestDigitalObject.DIGITAL_OBJECT_ID.getValue());
    }

    @Test
    @Transactional
    public void datastreamViewContainsExpectedProjectAbbr(){
      Assertions.assertThat(response).contains(TestProject.PROJECT_ABBR.getValue());
    }

    @Test
    @Transactional
    public void testDatastreamViewContainsExpectedDatastreamId(){
      Assertions.assertThat(response).contains(TestDatastream.DSID.getValue());
    }

    @Test
    @Transactional
    public void testDatastreamViewContainsExpectedMimeType(){
      Assertions.assertThat(response).contains(TestDatastream.MIME_TYPE.getValue());
    }

    @Test
    @Transactional
    public void testDatastreamViewContainsExpectedFileName(){
      Assertions.assertThat(response).contains(TestDatastream.FILE_NAME.getValue());
    }

    @Test
    @Transactional
    public void testDatastreamViewContainsExpectedTags(){
      TestDatastream.DATASTREAM_TAGS.forEach(tag -> Assertions.assertThat(response).contains(tag));
    }

    @Test
    @Transactional
    public void testDatastreamViewContainsExpectedBaseMetadataTitle(){
      Assertions.assertThat(response).contains(TestDatastream.METADATA_BASE_ENTITY.getTitle());
    }

    @Test
    @Transactional
    public void testDatastreamViewContainsExpectedBaseMetadataDescription(){
      Assertions.assertThat(response).contains(TestDatastream.METADATA_BASE_ENTITY.getDescription());
    }

    @Test
    @Transactional
    public void testDatastreamViewContainsExpectedBaseMetadataCreator(){
      Assertions.assertThat(response).contains(TestDatastream.METADATA_BASE_ENTITY.getCreator());
    }

    @Test
    @Transactional
    public void testDatastreamViewContainsExpectedLang(){
      TestDatastream.DATASTREAM_LANG.forEach(lang -> Assertions.assertThat(response).contains(lang));
    }

  }

  @Nested
  public class FailedIngestShouldHaveNoSideEffectsTests {

    @Test
    public void doesNotCreateDatastreamContentIfProjectWasNotFound() throws Exception {

      final String NOT_EXISTING_PROJECT_ABBR = "NOT_EXISTING";

      byte[] zippedBag = ZipUtils.zipDir(bagFile);
      MockPart mockPart = new MockPart(IngestStatics.FORM_PART_NAME.name, "test.zip", zippedBag);
      mockMvc
          .perform(
              multipart("/api/v1/projects/{projectAbbr}/objects", NOT_EXISTING_PROJECT_ABBR)
                  .part(mockPart)
          )
          .andExpect(status().is4xxClientError());

      Assertions.assertThat(
          datastreamContentRepository.exists(DatastreamId.builder()
              .digitalObject(TestDigitalObject.DIGITAL_OBJECT_ID.getValue())
              .dsid(TestDatastream.DSID.getValue())
              .build()
          )
      ).isFalse();

    }

  }

}