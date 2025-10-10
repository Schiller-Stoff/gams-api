package org.zim.gamsapi.application.Ingest;

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
import org.zim.gamsapi.domain.Datastream.DatastreamId;
import org.zim.gamsapi.domain.Datastream.utils.interfaces.IDatastreamRepository;
import org.zim.gamsapi.domain.Datastream.utils.interfaces.IDatastreamContentRepository;
import org.zim.gamsapi.domain.DigitalObject.utils.interfaces.IDigitalObjectRepository;
import org.zim.gamsapi.application.Ingest.utils.Bagit.BagFilePaths;
import org.zim.gamsapi.application.Ingest.utils.IngestStatics;
import org.zim.gamsapi.application.Ingest.utils.ZipUtils;
import org.zim.gamsapi.IntegrationTest;
import org.zim.gamsapi.domain.Project.interfaces.IProjectRepository;
import org.zim.gamsapi.TestUtilities.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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

  @Autowired
  private TestDataBuilder testDataBuilder;

  // disables auditing
  @MockitoBean
  private AuditingHandler auditingHandler;

  File bagFile;

  @BeforeEach
  public void setup() throws IOException {
    bagFile = TestBag.loadFile();
    projectRepository.save(TestProject.generate());
  }


  @Nested
  public class IngestBaseSuccessTests {

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
    public void ingestFailsIfProjectAbbrDiffersFromBagItSipJSONProject() throws Exception {
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
        Assertions.assertThat(response).contains(String.format(">%s<", TestDigitalObject.DIGITAL_OBJECT_MAIN_RESOURCE.getValue()));
        //match expected datastream id two times (once in list overview / once as main-resource)
        Assertions.assertThat(response).containsPattern(
            String.format("(%s.*?){2}", TestDigitalObject.DIGITAL_OBJECT_MAIN_RESOURCE.getValue())
        );

      }

    }


    @Nested
    public class GETJSONAssertions {

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
                    .accept(MediaType.APPLICATION_JSON)
                    .contentType(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().isOk())
            .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andReturn();

        response = mvcResult.getResponse().getContentAsString();

      }

      @Test
      @Transactional // ensures that mainDatastream tags / lang loading is working (Hibernate lazy loading)
      public void responseContainsExpectedMainResourceMetadata(){

        var mainDatastreams = datastreamRepository.findMainDatastreamsByDigitalObjectIds(
            Set.of(TestDigitalObject.DIGITAL_OBJECT_ID.getValue())
        );

        Assertions.assertThat(mainDatastreams)
            .isNotNull()
            .hasSize(1);

        var mainDatastream = mainDatastreams.get(0);

        Assertions.assertThat(response).contains(
            mainDatastream.getDsid(),
            mainDatastream.getMimeType(),
            mainDatastream.getBaseMetadata().getCreator(),
            mainDatastream.getBaseMetadata().getTitle(),
            mainDatastream.getBaseMetadata().getDescription(),
            mainDatastream.getBaseMetadata().getRights()
        );

        Assertions.assertThat(response).contains(mainDatastream.getTags());
        Assertions.assertThat(response).contains(mainDatastream.getLang());

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

  @Nested
  public class ExportBag {

    private TestDataSet testDataSet;

    @BeforeEach
    public void setup() {
      testDataSet = testDataBuilder.buildTestDataSet();
    }

    @Test
    public void exportedBagIsNotNullOrEmpty() throws Exception {

      MvcResult result = mockMvc.perform(
              MockMvcRequestBuilders.get("/api/v1/projects/{projectAbbr}/objects/{id}/export",
                      testDataSet.project().getProjectAbbr(),
                      testDataSet.digitalObject().getId())
                  .accept("application/zip")
          )
          .andExpect(status().isOk())
          .andExpect(MockMvcResultMatchers.header().string("Content-Type", "application/zip"))
          .andExpect(MockMvcResultMatchers.header().exists("Content-Disposition"))
          .andReturn();

      byte[] exportedZip = result.getResponse().getContentAsByteArray();
      org.assertj.core.api.Assertions.assertThat(exportedZip).isNotEmpty();

    }

    @Test
    public void exportedBagContainsExpectedValues() throws Exception {

      MvcResult result = mockMvc.perform(
              MockMvcRequestBuilders.get("/api/v1/projects/{projectAbbr}/objects/{id}/export",
                      testDataSet.project().getProjectAbbr(),
                      testDataSet.digitalObject().getId())
                  .accept("application/zip")
          )
          .andExpect(status().isOk())
          .andExpect(MockMvcResultMatchers.header().string("Content-Type", "application/zip"))
          .andExpect(MockMvcResultMatchers.header().exists("Content-Disposition"))
          .andReturn();

      byte[] exportedZip = result.getResponse().getContentAsByteArray();
      org.assertj.core.api.Assertions.assertThat(exportedZip).isNotEmpty();


      List<String> containedFileNames = new ArrayList<>();

      ZipUtils.walkZippedDir(exportedZip, (zipEntry, bos) -> {
        String fileName = Path.of(zipEntry.getName()).getFileName().toString();
        containedFileNames.add(fileName);

        switch (fileName){
          case "bagit.txt" -> {
            String bagitTxt = bos.toString();
            org.assertj.core.api.Assertions.assertThat(bagitTxt)
                .contains(testDataSet.submissionRecord().getBagVersion())
                .contains(testDataSet.submissionRecord().getBagTagFileCharacterEncoding());
          }
          case "bag-info.txt" -> {
            String bagInfoTxt = bos.toString();
            org.assertj.core.api.Assertions.assertThat(bagInfoTxt)
                .contains(testDataSet.submissionRecord().getBagExternalDescription())
                .contains(testDataSet.submissionRecord().getBaggingDate())
                .contains(testDataSet.submissionRecord().getBaggingTime())
                .contains(testDataSet.submissionRecord().getBagContactMail())
                .contains("Payload-Oxum: ")
            // Payload oxum cannot be the same because the test data ingest not the complete bag
            //.contains(testDataSet.ingestRecord().getBagPayloadOxum().toString())
            ;
          }
          case "manifest-md5.txt" -> {
            String manifestMd5Txt = bos.toString();
            org.assertj.core.api.Assertions.assertThat(manifestMd5Txt)
                .contains(testDataSet.mainDatastream().getBaseMetadata().getMd5Checksum())
                .contains(testDataSet.mainDatastream().getDsid())
                .contains(BagFilePaths.BAG_SIP_JSON.name)
            ;
          }
          case "manifest-sha512.txt" -> {
            String manifestSha512Txt = bos.toString();
            org.assertj.core.api.Assertions.assertThat(manifestSha512Txt)
                .contains(testDataSet.mainDatastream().getBaseMetadata().getSha512Checksum())
                .contains(testDataSet.mainDatastream().getDsid())
                .contains(BagFilePaths.BAG_SIP_JSON.name)
            ;
          }
          case "sip.json" -> {
            String sipJson = bos.toString();
            org.assertj.core.api.Assertions.assertThat(sipJson)
                .contains(testDataSet.digitalObject().getId())
                .contains(testDataSet.digitalObject().getProject().getProjectAbbr())
                .contains(testDataSet.digitalObject().getObjectType())
                .contains(testDataSet.digitalObject().getFunder())
                .contains(testDataSet.digitalObject().getMainResource())
                // base metadata
                .contains(testDataSet.digitalObject().getBaseMetadata().getTitle())
                .contains(testDataSet.digitalObject().getBaseMetadata().getDescription())
                .contains(testDataSet.digitalObject().getBaseMetadata().getCreator())
                .contains(testDataSet.digitalObject().getBaseMetadata().getRights())
                .contains(testDataSet.digitalObject().getPublisher())
                // assertions from ingestRecord
                .contains(testDataSet.submissionRecord().getBagSource())
                .contains(testDataSet.submissionRecord().getBagSchema())
                .contains(testDataSet.submissionRecord().getBagCreatedBy());

            // assertions for content files
            org.assertj.core.api.Assertions.assertThat(sipJson)
                .contains(testDataSet.mainDatastream().getDsid())
                .contains(testDataSet.mainDatastream().getTags())
                .contains(testDataSet.mainDatastream().getLang())
                .contains(testDataSet.mainDatastream().getMimeType())
                .contains(testDataSet.mainDatastream().getSize().toString())
                .contains(testDataSet.mainDatastream().getBagPath());

            var datastreamLang = testDataSet.mainDatastream().getLang();
            for (String lang : datastreamLang) {
              org.assertj.core.api.Assertions.assertThat(sipJson).contains(lang);
            }
            var datastreamTags = testDataSet.mainDatastream().getTags();
            for (String tag : datastreamTags) {
              org.assertj.core.api.Assertions.assertThat(sipJson).contains(tag);
            }

          }
          default -> {
            // other files are the datastreams - no content check here
          }

        }

      });


      org.assertj.core.api.Assertions.assertThat(containedFileNames)
          .isNotEmpty()
          .contains("bagit.txt", "bag-info.txt", "manifest-md5.txt", "sip.json", "manifest-sha512.txt")
          .contains(testDataSet.mainDatastream().getDsid());

    }



  }
}