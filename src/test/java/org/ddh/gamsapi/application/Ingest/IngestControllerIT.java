package org.ddh.gamsapi.application.Ingest;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.Assertions;
import org.ddh.gamsapi.TestUtilities.*;
import org.ddh.gamsapi.application.Ingest.utils.Bagit.mapping.BagSipJson;
import org.ddh.gamsapi.infrastructure.System.security.IUserPrincipalAuditorMapping;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.auditing.AuditingHandler;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockPart;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.transaction.annotation.Transactional;
import org.ddh.gamsapi.domain.Datastream.DatastreamId;
import org.ddh.gamsapi.domain.Datastream.utils.interfaces.IDatastreamRepository;
import org.ddh.gamsapi.domain.Datastream.utils.interfaces.IDatastreamContentRepository;
import org.ddh.gamsapi.domain.DigitalObject.utils.interfaces.IDigitalObjectRepository;
import org.ddh.gamsapi.application.Ingest.utils.Bagit.BagFilePaths;
import org.ddh.gamsapi.application.Ingest.utils.IngestStatics;
import org.ddh.gamsapi.application.Ingest.utils.ZipUtils;
import org.ddh.gamsapi.IntegrationTest;
import org.ddh.gamsapi.domain.Project.interfaces.IProjectRepository;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

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

  /**
   * Classes need to mock authenticated users when changing datastreams
   */
  @MockitoBean
  private AuditingHandler auditingHandler;
  @MockitoBean
  private IUserPrincipalAuditorMapping userPrincipalAuditorMapping;

  File bagFile;

  @BeforeEach
  public void setup() throws IOException {
    Mockito.when(userPrincipalAuditorMapping.getCurrentAuditor())
        .thenReturn(Optional.of("test-user"));
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
      public void digitalObjectViewContainsExpectedTags(){
        TestDigitalObject.getTags().forEach(tag -> Assertions.assertThat(response).contains(tag));
      }

      @Test
      public void digitalObjectViewContainsExpectedMainResource(){
        // check if label of main resource is there
        Assertions.assertThat(response).contains("Main resource");
        // check if the value of the main resource is there
        Assertions.assertThat(response).contains(String.format(">%s<", TestDigitalObject.DIGITAL_OBJECT_MAIN_RESOURCE.getValue()));
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
    public void testDatastreamViewContainsExpectedArchivalPolicy(){
      Assertions.assertThat(response).contains(TestDatastream.ARCHIVAL_POLICY.name());
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
                .contains(testDataSet.submissionRecord().getBagContactMail())
                .contains("Payload-Oxum: ")
            // Payload oxum cannot be the same because the test data ingest not the complete bag
            //.contains(testDataSet.ingestRecord().getBagPayloadOxum().toString())
            ;
          }
          case "manifest-md5.txt" -> {
            String manifestMd5Txt = bos.toString();
            org.assertj.core.api.Assertions.assertThat(manifestMd5Txt)
                .contains(testDataSet.mainDatastream().getMd5Checksum())
                .contains(testDataSet.mainDatastream().getDsid())
                .contains(BagFilePaths.BAG_SIP_JSON.name)
            ;
          }
          case "manifest-sha512.txt" -> {
            String manifestSha512Txt = bos.toString();
            org.assertj.core.api.Assertions.assertThat(manifestSha512Txt)
                .contains(testDataSet.mainDatastream().getSha512Checksum())
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
                // does not contain createdBy because the ingest user is different from the bag-creator - is the gams-api in this case
                .doesNotContain(testDataSet.submissionRecord().getBagCreatedBy());

            // assertions for digital object tags
            testDataSet.digitalObject().getTags().forEach(tag -> {
              Assertions.assertThat(sipJson).contains(tag);
            });

            // assertions for content files
            org.assertj.core.api.Assertions.assertThat(sipJson)
                .contains(testDataSet.mainDatastream().getDsid())
                .contains(testDataSet.mainDatastream().getTags())
                .contains(testDataSet.mainDatastream().getLang())
                .contains(testDataSet.mainDatastream().getMimeType())
                .contains(testDataSet.mainDatastream().getSize().toString())
                .contains(testDataSet.mainDatastream().getFilePath());

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

  @Nested
  public class BagIngestExport {

    @Test
    public void ingestedBagShouldSemanticallyMatchExportedBag() throws Exception {
      // --- 1. SETUP & INGEST ---
      File originalBagFile = TestBag.loadFile();
      byte[] zippedImportBag = ZipUtils.zipDir(originalBagFile);
      String projectAbbr = TestProject.PROJECT_ABBR.getValue();
      String objectId = TestDigitalObject.DIGITAL_OBJECT_ID.getValue();

      MockPart mockPart = new MockPart(IngestStatics.FORM_PART_NAME.name, "test.zip", zippedImportBag);
      mockMvc.perform(multipart("/api/v1/projects/{projectAbbr}/objects", projectAbbr).part(mockPart))
          .andExpect(status().isOk());

      // --- 2. EXPORT ---
      MvcResult exportResult = mockMvc.perform(MockMvcRequestBuilders.get(
                  "/api/v1/projects/{projectAbbr}/objects/{id}/export", projectAbbr, objectId)
              .accept("application/zip"))
          .andExpect(status().isOk())
          .andReturn();

      byte[] exportedZipBytes = exportResult.getResponse().getContentAsByteArray();

      // --- 3. EXTRACTION ---
      ObjectMapper mapper = new ObjectMapper();
      AtomicReference<BagSipJson> exportedSipRef = new AtomicReference<>();
      AtomicReference<String> exportedManifestMd5 = new AtomicReference<>();
      AtomicReference<String> exportedManifestSha512 = new AtomicReference<>();

      ZipUtils.walkZippedDir(exportedZipBytes, (zipEntry, bos) -> {
        String fullEntryName = zipEntry.getName();
        try {
          if (fullEntryName.endsWith(BagFilePaths.BAG_SIP_JSON.name)) {
            exportedSipRef.set(mapper.readValue(bos.toByteArray(), BagSipJson.class));
          } else if (fullEntryName.endsWith("manifest-md5.txt")) {
            exportedManifestMd5.set(bos.toString(StandardCharsets.UTF_8));
          } else if (fullEntryName.endsWith("manifest-sha512.txt")) {
            exportedManifestSha512.set(bos.toString(StandardCharsets.UTF_8));
          }
        } catch (Exception e) {
          throw new RuntimeException("Failed to parse zip entry: " + fullEntryName, e);
        }
      });

      // --- 4. ASSERT SIP.JSON DOMAIN LOGIC ---
      File originalSipJsonFile = new File(originalBagFile, BagFilePaths.BAG_SIP_JSON.name);
      BagSipJson originalSip = mapper.readValue(originalSipJsonFile, BagSipJson.class);
      BagSipJson exportedSip = exportedSipRef.get();

      Assertions.assertThat(exportedSip).as("Exported sip.json should be present in the zip").isNotNull();
      Assertions.assertThat(exportedSip.getRecid()).isEqualTo(originalSip.getRecid());
      Assertions.assertThat(exportedSip.getCreated_by()).isNotEqualTo(originalSip.getCreated_by()).contains("gams-api");
      // ... (Keep existing domain field assertions here) ...

      // --- 5. ASSERT MANIFEST CHECKSUMS ---
      // Read original manifests from the test filesystem
      String originalMd5Content = java.nio.file.Files.readString(
          Path.of(originalBagFile.getAbsolutePath(), BagFilePaths.MANIFEST_MD5_FILE_PATH.name));
      String originalSha512Content = java.nio.file.Files.readString(
          Path.of(originalBagFile.getAbsolutePath(), BagFilePaths.MANIFEST_SHA512_FILE_PATH.name));

      // Parse into Maps
      Map<String, String> originalMd5Map = parseManifest(originalMd5Content);
      Map<String, String> exportedMd5Map = parseManifest(exportedManifestMd5.get());

      Map<String, String> originalSha512Map = parseManifest(originalSha512Content);
      Map<String, String> exportedSha512Map = parseManifest(exportedManifestSha512.get());

      // Assert rules
      assertManifestsMatchExceptSipJson(originalMd5Map, exportedMd5Map, "MD5");
      assertManifestsMatchExceptSipJson(originalSha512Map, exportedSha512Map, "SHA-512");
    }

    /**
     * Parses a BagIt manifest file content into a Map of FilePath -> Checksum.
     */
    private Map<String, String> parseManifest(String manifestContent) {
      Map<String, String> manifestMap = new HashMap<>();
      if (manifestContent == null || manifestContent.isBlank()) {
        return manifestMap;
      }

      String[] lines = manifestContent.split("\\r?\\n");
      for (String line : lines) {
        if (line.trim().isEmpty()) continue;
        // Split by whitespace. Limit to 2 parts: [0]=checksum, [1]=filepath
        String[] parts = line.trim().split("\\s+", 2);
        if (parts.length == 2) {
          manifestMap.put(parts[1], parts[0]);
        }
      }
      return manifestMap;
    }

    /**
     * Asserts that all files in the original manifest match the exported manifest,
     * EXCEPT for the sip.json which must exist but have a different checksum.
     */
    private void assertManifestsMatchExceptSipJson(Map<String, String> original, Map<String, String> exported, String manifestType) {
      Assertions.assertThat(exported)
          .as(manifestType + " manifest should contain the same number of file entries")
          .hasSameSizeAs(original);

      for (Map.Entry<String, String> entry : original.entrySet()) {
        String filePath = entry.getKey();
        String originalChecksum = entry.getValue();
        String exportedChecksum = exported.get(filePath);

        Assertions.assertThat(exportedChecksum)
            .as("File " + filePath + " is missing from the exported " + manifestType + " manifest")
            .isNotNull();

        if (filePath.equals(BagFilePaths.BAG_SIP_JSON.name)) {
          Assertions.assertThat(exportedChecksum)
              .as("Checksum for " + filePath + " in " + manifestType + " MUST DIFFER because the file is updated by gams-api during export")
              .isNotEqualTo(originalChecksum);
        } else {
          Assertions.assertThat(exportedChecksum)
              .as("Checksum for " + filePath + " in " + manifestType + " MUST MATCH exactly")
              .isEqualTo(originalChecksum);
        }
      }
    }

  }

}