package org.ddh.gamsapi.application.Ingest;

import org.assertj.core.api.Assertions;
import org.ddh.gamsapi.TestUtilities.*;
import org.ddh.gamsapi.application.Integration.CustomSearch.CustomSearchProperties;
import org.ddh.gamsapi.application.Integration.PlexusSearch.PlexusSearchProperties;
import org.ddh.gamsapi.infrastructure.System.security.IUserPrincipalAuditorMapping;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.auditing.AuditingHandler;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.ddh.gamsapi.EventCaptureListener;
import org.ddh.gamsapi.IntegrationTest;
import org.ddh.gamsapi.application.Ingest.exceptions.IngestObjectAlreadyExistsException;
import org.ddh.gamsapi.application.Ingest.utils.Bagit.BagFilePaths;
import org.ddh.gamsapi.application.Ingest.utils.ZipUtils;
import org.ddh.gamsapi.domain.Datastream.DatastreamId;
import org.ddh.gamsapi.domain.Datastream.utils.interfaces.IDatastreamContentRepository;
import org.ddh.gamsapi.domain.Datastream.utils.interfaces.IDatastreamRepository;
import org.ddh.gamsapi.domain.DigitalObject.utils.events.DigitalObjectCreatedEvent;
import org.ddh.gamsapi.domain.DigitalObject.DublinCoreEntry.DublinCoreEntrySummaryView;
import org.ddh.gamsapi.domain.DigitalObject.DublinCoreEntry.IDublinCoreEntryRepository;
import org.ddh.gamsapi.domain.DigitalObject.SubmissionRecord.ISubmissionRecordRepository;
import org.ddh.gamsapi.domain.DigitalObject.utils.interfaces.IDigitalObjectRepository;
import org.ddh.gamsapi.domain.Project.ProjectBuilder;
import org.ddh.gamsapi.domain.Project.interfaces.IProjectRepository;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class IngestServiceIT extends IntegrationTest {

  @Autowired
  IProjectRepository projectRepository;

  @Autowired
  IDigitalObjectRepository digitalObjectRepository;

  @Autowired
  IDatastreamRepository datastreamRepository;

  @Autowired
  IDatastreamContentRepository datastreamContentRepository;

  @Autowired
  IDublinCoreEntryRepository dublinCoreElementRepository;

  @Autowired
  ISubmissionRecordRepository bagEntityRepository;

  @Autowired
  IngestService ingestService;

  @Autowired
  private EventCaptureListener eventCaptureListener;

  File bagFile;

  /**
   * Classes need to mock authenticated users when changing datastreams
   */
  @MockitoBean
  private AuditingHandler auditingHandler;
  @MockitoBean
  private IUserPrincipalAuditorMapping userPrincipalAuditorMapping;

  @BeforeEach
  public void setup(){
    Mockito.when(userPrincipalAuditorMapping.getCurrentAuditor())
        .thenReturn(Optional.of(TestUser.USERNAME.getValue()));
  }

  @Nested
  public class IngestUpdatesProjectModified {


    @Test
    public void ingestUpdatesProjectModifiedProperties() throws IOException {

      projectRepository.save(ProjectBuilder.builder().projectAbbr(TestProject.PROJECT_ABBR.getValue()).build());

      // get the project before ingest
      var project = projectRepository.findById(TestProject.PROJECT_ABBR.getValue())
          .orElseThrow( () -> new RuntimeException("GAMS Project not found"));
      var lastModifiedBeforeIngest = project.getModified();

      bagFile = TestBag.loadFile();

      // ingest the bag
      byte[] zippedBag = ZipUtils.zipDir(bagFile);
      ingestService.ingest(
          TestProject.PROJECT_ABBR.getValue(),
          new ByteArrayInputStream(zippedBag)
      );

      // get the project after ingest
      var updatedProject = projectRepository.findById(TestProject.PROJECT_ABBR.getValue())
          .orElseThrow();
      var lastModifiedAfterIngest = updatedProject.getModified();

      Assertions.assertThat(lastModifiedAfterIngest)
          .isNotNull()
          .isAfter(lastModifiedBeforeIngest);

      // check if modifiedBy was updated
      final var ORIGINAL_MODIFIED_BY = project.getModifiedBy();
      Assertions.assertThat(ORIGINAL_MODIFIED_BY).isNotEqualTo(TestUser.USERNAME.getValue());
      Assertions.assertThat(updatedProject.getModifiedBy())
          .isEqualTo(TestUser.USERNAME.getValue());
    }
  }


  @Nested
  public class IngestCreatesExpectedObjects {
    @BeforeEach
    public void setup() throws IOException {
      bagFile = TestBag.loadFile();
      projectRepository.save(ProjectBuilder.builder().projectAbbr(TestProject.PROJECT_ABBR.getValue()).build());

      // ingest the bag
      byte[] zippedBag = ZipUtils.zipDir(bagFile);
      ingestService.ingest(
          TestProject.PROJECT_ABBR.getValue(),
          new ByteArrayInputStream(zippedBag)
      );
    }

    @Test
    public void createsExpectedDigitalObject_withDatastreamsAndContentAndDublinCore(){

      // assert that the digital object was created
      Assertions.assertThat(digitalObjectRepository.findAll()).isNotEmpty();
      var datastreams = datastreamRepository.findAll();
      Assertions.assertThat(datastreams)
          .isNotEmpty()
          .hasSize(7);

      // assert that expected datastream content exists on the fileystem
      datastreams.forEach(datastream -> {
        Assertions.assertThat(datastreamContentRepository.exists(datastream.deriveDatastreamId())).isTrue();
      });

      // assert that some dublin core elements were created
      Assertions.assertThat(
              dublinCoreElementRepository.count()
          )
          .isNotNull()
          .isNotEqualTo(0)
          .isGreaterThan(2);

    }

    @Test
    @Transactional
    public void createsDatastreamsWithServerComputedChecksums() {
      var datastreams = datastreamRepository.findAll();
      Assertions.assertThat(datastreams).isNotEmpty();

      datastreams.forEach(ds -> {
        Assertions.assertThat(ds.getMd5Checksum())
            .isNotNull()
            .isNotEmpty()
            .hasSize(32);

        Assertions.assertThat(ds.getSha512Checksum())
            .isNotNull()
            .isNotEmpty()
            .hasSize(128);
      });
    }

    @Test
    @Transactional
    public void createsDigitalObjectWithExpectedTagsSize(){
        var digitalObject = digitalObjectRepository.findById(TestDigitalObject.DIGITAL_OBJECT_ID.getValue())
            .orElseThrow( () -> new RuntimeException("Digital object not found"));
        Assertions.assertThat(digitalObject.getTags().size())
            .isEqualTo(TestDigitalObject.getTags().size());
    }

    @Test
    public void ingestCreatesDigitalObjectWithIngestedPropertyTrue(){
      var ingestedObject =  digitalObjectRepository.findById(TestDigitalObject.DIGITAL_OBJECT_ID.getValue());

      Assertions.assertThat(ingestedObject)
          .isNotNull()
          .isPresent();

      Assertions.assertThat(ingestedObject.get().isIngested())
          .isTrue();

    }

    @Test
    public void ingestCreatesDigitalObjectWithModifiedAfterCreationFalse(){
      var ingestedObject =  digitalObjectRepository.findById(TestDigitalObject.DIGITAL_OBJECT_ID.getValue());

      Assertions.assertThat(ingestedObject)
          .isNotNull()
          .isPresent();

      Assertions.assertThat(ingestedObject.get().isModifiedAfterCreation())
          .isFalse();
    }

    @Test
    public void createsExpectedDublinCoreEntryNamesForTestDigitalObject() {

      var dublinCoreEntries = dublinCoreElementRepository.findByDigitalObject(TestDigitalObject.generate());

      Assertions.assertThat(dublinCoreEntries)
          .isNotEmpty();

      List<String> foundDcElementNames = dublinCoreEntries.stream().map(DublinCoreEntrySummaryView::getName).toList();

      Assertions.assertThat(foundDcElementNames)
          .contains(
              "title",
              "relation",
              "creator",
              "contributor",
              "date",
              "format",
              "language",
              "publisher",
              "source",
              "subject",
              "type");
    }

    @Test
    public void ingestCreatesExpectedBagEntityWithNoNullProperties(){
        var bagEntity = bagEntityRepository.findById(TestDigitalObject.DIGITAL_OBJECT_ID.getValue());
        Assertions.assertThat(bagEntity)
            .isPresent();

        Assertions.assertThat(bagEntity.get())
                .hasNoNullFieldsOrProperties();

    }

    @Test
    public void ingestCreatesExpectedMetadataBaseEntity(){
        var bagEntity = bagEntityRepository.findById(TestSubmissionRecord.ID);
        Assertions.assertThat(bagEntity)
            .isPresent();
        var foundBagEntity = bagEntity.get();

        Assertions.assertThat(foundBagEntity.getId()).isEqualTo(TestSubmissionRecord.ID);
        Assertions.assertThat(foundBagEntity.getBagCreatedBy()).isEqualTo(TestBag.TestBagSipJson.CREATED_BY);
        Assertions.assertThat(foundBagEntity.getBagSchema()).isEqualTo(TestBag.TestBagSipJson.SCHEMA);
        Assertions.assertThat(foundBagEntity.getBagSource()).isEqualTo(TestBag.TestBagSipJson.SOURCE);
        Assertions.assertThat(foundBagEntity.getBagExternalDescription()).isEqualTo(TestBag.TestBagInfo.EXTERNAL_DESCRIPTION);
        Assertions.assertThat(foundBagEntity.getBaggingDate()).isEqualTo(TestBag.TestBagInfo.BAGGING_DATE);
        Assertions.assertThat(foundBagEntity.getBagContactMail()).isEqualTo(TestBag.TestBagInfo.CONTACT_EMAIL);
        Assertions.assertThat(foundBagEntity.getBagPayloadOxum()).isEqualTo(TestBag.TestBagInfo.PAYLOAD_OXUM);

    }

    @Test
    public void ingestCreatesExpectedDatastreamCount(){

      var datastreams = datastreamRepository.findAll();
      Assertions.assertThat(datastreams)
          .isNotEmpty()
          .hasSize(7);

    }

    @Test
    @Transactional
    public void ingestCreatesDatastreamWithExpectedProperties(){

      var datastream = datastreamRepository.findById(
          DatastreamId.builder()
              .digitalObject(TestDigitalObject.DIGITAL_OBJECT_ID.getValue())
              .dsid(TestDatastream.DSID.getValue())
              .build()
      );

      Assertions.assertThat(datastream)
          .isPresent();

      var foundDatastream = datastream.get();

      Assertions.assertThat(foundDatastream.getMimeType())
          .isEqualTo(TestDatastream.MIME_TYPE.getValue());

      Assertions.assertThat(foundDatastream.getBaseMetadata().getTitle())
          .isEqualTo(TestDatastream.METADATA_BASE_ENTITY.getTitle());

      Assertions.assertThat(foundDatastream.getBaseMetadata().getDescription())
          .isEqualTo(TestDatastream.METADATA_BASE_ENTITY.getDescription());

      Assertions.assertThat(foundDatastream.getBaseMetadata().getCreator())
          .isEqualTo(TestDatastream.METADATA_BASE_ENTITY.getCreator());

      Assertions.assertThat(foundDatastream.getBaseMetadata().getRights())
          .isEqualTo(TestDatastream.METADATA_BASE_ENTITY.getRights());

      Assertions.assertThat(foundDatastream.getBagPath())
          .isEqualTo(TestDatastream.BAG_PATH.getValue());

      Assertions.assertThat(foundDatastream.getLang().size()).isEqualTo(TestDatastream.DATASTREAM_LANG.size());
      Assertions.assertThat(foundDatastream.getTags().size()).isEqualTo(TestDatastream.DATASTREAM_TAGS.size());

      Assertions.assertThat(foundDatastream.getMd5Checksum())
          .isEqualTo(TestDatastream.MD5_CHECKSUM);

      Assertions.assertThat(foundDatastream.getSha512Checksum())
          .isEqualTo(TestDatastream.SHA512_CHECKSUM);


    }

    @Nested
    public class DublinCoreEntries {

      @Test
      public void testObjectHasDublinCoreTitleEntryWithExpectedLanguage(){

        var dublinCoreEntries = dublinCoreElementRepository.findByDigitalObject(
            TestDigitalObject.generate()
        );

        Assertions.assertThat(dublinCoreEntries)
            .isNotEmpty();

        // there should be a dublin core entry with name "title" AND language "en"
        Assertions.assertThat(dublinCoreEntries)
            .anySatisfy(dcEntry -> {
              Assertions.assertThat(dcEntry.getLanguage()).isNotNull();
              Assertions.assertThat(dcEntry.getName()).isEqualTo("title");
              Assertions.assertThat(dcEntry.getLanguage()).isEqualTo("en");
            });
      }

    }

    @Test
    public void ingestCreatesObjectWithExpectedTags(){

      var digitalObject = digitalObjectRepository.findById(TestDigitalObject.DIGITAL_OBJECT_ID.getValue())
          .orElseThrow(
              () -> new RuntimeException("Digital object not found")
          );

      Assertions.assertThat(digitalObject.getTags())
          .isNotEmpty();

      Assertions.assertThat(digitalObject.getTags().size())
          .isEqualTo(TestDigitalObject.getTags().size());

      digitalObject.getTags().forEach(
          tag -> Assertions.assertThat(TestDigitalObject.getTags()).contains(tag)
      );


    }

  }

  @Nested
  public class IngestTriggersExpectedEvents {

    @Test
    public void ingestShouldTriggerDigitalObjectCreatedEventExactlyOnce() throws IOException {

      bagFile = TestBag.loadFile();
      projectRepository.save(ProjectBuilder.builder().projectAbbr(TestProject.PROJECT_ABBR.getValue()).build());

      // ingest the bag
      byte[] zippedBag = ZipUtils.zipDir(bagFile);
      ingestService.ingest(
          TestProject.PROJECT_ABBR.getValue(),
          new ByteArrayInputStream(zippedBag)
      );


      long eventCont = eventCaptureListener.countEventsOfType(DigitalObjectCreatedEvent.class);

      Assertions.assertThat(eventCont)
          .isEqualTo(1);

    }

  }

  @Nested
  public class IngestErrors {

    @Test
    public void ingestThrowsIfDigitalObjectAlreadyExists() throws IOException {

      bagFile = TestBag.loadFile();
      projectRepository.save(ProjectBuilder.builder().projectAbbr(TestProject.PROJECT_ABBR.getValue()).build());

      // save object first (should result in conflict)
      digitalObjectRepository.save(TestDigitalObject.generate());

      // ingest the bag
      byte[] zippedBag = ZipUtils.zipDir(bagFile);
      Assertions.assertThatThrownBy(
          () -> ingestService.ingest(
              TestProject.PROJECT_ABBR.getValue(),
              new ByteArrayInputStream(zippedBag)
          )
      ).isInstanceOf(IngestObjectAlreadyExistsException.class);

    }


  }

  @Nested
  public class ExportBag {

    @BeforeEach
    public void setup() throws IOException {
      bagFile = TestBag.loadFile();
      projectRepository.save(ProjectBuilder.builder().projectAbbr(TestProject.PROJECT_ABBR.getValue()).build());

      // ingest the bag
      byte[] zippedBag = ZipUtils.zipDir(bagFile);
      ingestService.ingest(
          TestProject.PROJECT_ABBR.getValue(),
          new ByteArrayInputStream(zippedBag)
      );

    }

    @Test
    public void exportedBagHasExpectedStructure() throws IOException {

      try (
          var outputStream = new ByteArrayOutputStream()
      ) {
        ingestService.exportAsBag(TestDigitalObject.DIGITAL_OBJECT_ID.getValue(), outputStream);

        // read output stream as zip and check structure
        var zipBytes = outputStream.toByteArray();

        List<String> entryNames = new ArrayList<>();

        ZipUtils.walkZippedDir(zipBytes, (zipEntry, byteArrayOutputStream) -> {
          Assertions.assertThat(zipEntry.getName()).isNotBlank();
          Assertions.assertThat(byteArrayOutputStream.size()).isGreaterThan(0);
          entryNames.add(zipEntry.getName());

          // read content of some expected files
          // zipEntry is a path -> only take filename for switch
          String fileName = Path.of(zipEntry.getName()).getFileName().toString();

          switch (fileName) {
            case "bagit.txt" -> {
              String bagitTxtContent = byteArrayOutputStream.toString();
              Assertions.assertThat(bagitTxtContent).contains(TestBag.BagitTxt.BAGIT_VERSION);
              Assertions.assertThat(bagitTxtContent).contains(TestBag.BagitTxt.TAG_FILE_CHARACTER_ENCODING);
            }
            case "bag-info.txt" -> {
              String bagInfoTxtContent = byteArrayOutputStream.toString();
              Assertions.assertThat(bagInfoTxtContent).contains(TestBag.TestBagInfo.EXTERNAL_DESCRIPTION);
              Assertions.assertThat(bagInfoTxtContent).contains(TestBag.TestBagInfo.BAGGING_DATE);
              Assertions.assertThat(bagInfoTxtContent).contains(TestBag.TestBagInfo.CONTACT_EMAIL);
              // reconstructed payload oxum may differ from original due to different line endings (windows vs unix etc.)
              // so we are only checking for the label here
              // Assertions.assertThat(bagInfoTxtContent).contains(TestBag.TestBagInfo.PAYLOAD_OXUM.toString());
              Assertions.assertThat(bagInfoTxtContent).contains("Payload-Oxum:");
            }
            case "sip.json" -> {
              String sipJsonContent = byteArrayOutputStream.toString();
              Assertions.assertThat(sipJsonContent).isNotBlank();
              Assertions.assertThat(sipJsonContent).contains("recid"); // field name of id

              Assertions.assertThat(sipJsonContent).contains(TestBag.TestBagSipJson.REC_ID);
              Assertions.assertThat(sipJsonContent).contains(TestBag.TestBagSipJson.PROJECT);
              // createdBy is different for the export than for ingest (it is set to the system user during export)
              Assertions.assertThat(sipJsonContent).contains("created_by");
              Assertions.assertThat(sipJsonContent).doesNotContain(TestBag.TestBagSipJson.CREATED_BY);
              Assertions.assertThat(sipJsonContent).contains("$schema"); // field name of id
              Assertions.assertThat(sipJsonContent).contains(TestBag.TestBagSipJson.SCHEMA);
              Assertions.assertThat(sipJsonContent).contains(TestBag.TestBagSipJson.SOURCE);
              Assertions.assertThat(sipJsonContent).contains(TestBag.TestBagSipJson.CREATOR);
              Assertions.assertThat(sipJsonContent).contains(TestBag.TestBagSipJson.TITLE);
              Assertions.assertThat(sipJsonContent).contains(TestBag.TestBagSipJson.DESCRIPTION);
              Assertions.assertThat(sipJsonContent).contains(TestBag.TestBagSipJson.PUBLISHER);
              Assertions.assertThat(sipJsonContent).contains(TestBag.TestBagSipJson.FUNDER);
              Assertions.assertThat(sipJsonContent).contains(TestBag.TestBagSipJson.MAIN_RESOURCE);
              // tags contained in sip json
              TestBag.TestBagSipJson.DIGITAL_OBJECT_TAGS.forEach(tag -> {
                Assertions.assertThat(sipJsonContent).contains(tag);
              });


              // assertions about test datastream
              Assertions.assertThat(sipJsonContent)
                  .contains(
                      TestDatastream.DSID.getValue(),
                      TestDatastream.BAG_PATH.getValue(),
                      TestDatastream.MIME_TYPE.getValue(),
                      TestDatastream.METADATA_BASE_ENTITY.getTitle(),
                      TestDatastream.METADATA_BASE_ENTITY.getDescription(),
                      TestDatastream.METADATA_BASE_ENTITY.getCreator(),
                      TestDatastream.METADATA_BASE_ENTITY.getRights()
                  );
              // test datastream tags + langs
              // transform set to list for assertion
              var datastreamLangs = new ArrayList<>(TestDatastream.DATASTREAM_LANG);
              Assertions.assertThat(sipJsonContent).contains(datastreamLangs);
              var datastreamTags = new ArrayList<>(TestDatastream.DATASTREAM_TAGS);
              Assertions.assertThat(sipJsonContent).contains(datastreamTags);

              Assertions.assertThat(sipJsonContent).doesNotContain(
                  // sip json must not contain checksums
                  "md5Checksum",
                  "sha512Checksum",
                  // in the test data there should not be any escaped quotes
                  "\\\""
              );


            }
            case "manifest-md5.txt" -> {
              String manifestMd5Content = byteArrayOutputStream.toString();
              Assertions.assertThat(manifestMd5Content).contains(BagFilePaths.BAG_SIP_JSON.name);
              Assertions.assertThat(manifestMd5Content).contains(BagFilePaths.DUBLIN_CORE_XML.name);
              Assertions.assertThat(manifestMd5Content).contains(TestDatastream.MD5_CHECKSUM);
            }
            case "manifest-sha512.txt" -> {
              String manifestSha512Content = byteArrayOutputStream.toString();
              Assertions.assertThat(manifestSha512Content).contains(BagFilePaths.DUBLIN_CORE_XML.name);
              Assertions.assertThat(manifestSha512Content).contains(BagFilePaths.BAG_SIP_JSON.name);
              Assertions.assertThat(manifestSha512Content).contains(TestDatastream.SHA512_CHECKSUM);
            }

            default -> {
              // do nothing
            }
          }
        });


        Assertions.assertThat(entryNames.size()).isEqualTo(12);

        // Assert presence of generated files
        Assertions.assertThat(entryNames).contains(
            String.format("%s/bagit.txt", TestDigitalObject.DIGITAL_OBJECT_ID.getValue()),
            String.format("%s/bag-info.txt", TestDigitalObject.DIGITAL_OBJECT_ID.getValue()),
            String.format("%s/manifest-md5.txt", TestDigitalObject.DIGITAL_OBJECT_ID.getValue()),
            String.format("%s/manifest-sha512.txt", TestDigitalObject.DIGITAL_OBJECT_ID.getValue()),
            String.format("%s/data/meta/sip.json", TestDigitalObject.DIGITAL_OBJECT_ID.getValue())

        );

        // Assert presence of datastream files
        Assertions.assertThat(entryNames).contains(
            String.format("%s/data/content/DC.xml", TestDigitalObject.DIGITAL_OBJECT_ID.getValue()),
            String.format("%s/data/content/test.xml", TestDigitalObject.DIGITAL_OBJECT_ID.getValue()),
            String.format("%s/data/content/test.txt", TestDigitalObject.DIGITAL_OBJECT_ID.getValue()),
            String.format("%s/data/content/manifest.json", TestDigitalObject.DIGITAL_OBJECT_ID.getValue()),
            String.format("%s/data/content/%s", TestDigitalObject.DIGITAL_OBJECT_ID.getValue(), CustomSearchProperties.DATASTREAM_DSID.name),
            String.format("%s/data/content/%s", TestDigitalObject.DIGITAL_OBJECT_ID.getValue(), PlexusSearchProperties.DATASTREAM_DSID.name)
        );



      }
    }

  }

}
