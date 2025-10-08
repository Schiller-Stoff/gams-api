package org.zim.gamsapi.Ingest;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.auditing.AuditingHandler;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.zim.gamsapi.Datastream.DatastreamId;
import org.zim.gamsapi.Datastream.interfaces.IDatastreamContentRepository;
import org.zim.gamsapi.Datastream.interfaces.IDatastreamRepository;
import org.zim.gamsapi.DigitalObject.DigitalObjectCreatedEvent;
import org.zim.gamsapi.DigitalObject.DublinCoreEntry.DublinCoreEntrySummaryView;
import org.zim.gamsapi.DigitalObject.DublinCoreEntry.IDublinCoreEntryRepository;
import org.zim.gamsapi.DigitalObject.IDigitalObjectRepository;
import org.zim.gamsapi.EventCaptureListener;
import org.zim.gamsapi.Ingest.exceptions.IngestObjectAlreadyExistsException;
import org.zim.gamsapi.Ingest.interfaces.IIngestRecordRepository;
import org.zim.gamsapi.Ingest.utils.ZipUtils;
import org.zim.gamsapi.IntegrationTest;
import org.zim.gamsapi.Project.ProjectBuilder;
import org.zim.gamsapi.Project.interfaces.IProjectRepository;
import org.zim.gamsapi.TestUtilities.*;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
  IIngestRecordRepository bagEntityRepository;

  @Autowired
  IngestService ingestService;

  @Autowired
  private EventCaptureListener eventCaptureListener;

  File bagFile;

  // disables auditing
  @MockitoBean
  private AuditingHandler auditingHandler;

  @Nested
  public class IngestUpdatesProjectContentLastModified {


    @Test
    public void ingestUpdatesProjectContentLastModified() throws IOException {

      projectRepository.save(ProjectBuilder.builder().projectAbbr(TestProject.PROJECT_ABBR.getValue()).build());

      // get the project before ingest
      var project = projectRepository.findById(TestProject.PROJECT_ABBR.getValue())
          .orElseThrow( () -> new RuntimeException("GAMS Project not found"));
      var lastModifiedBeforeIngest = project.getContentLastModified();

      bagFile = TestBag.loadFile();

      // ingest the bag
      byte[] zippedBag = ZipUtils.zipDir(bagFile);
      Ingest ingest = new Ingest();
      ingest.setZippedBagItFolder(zippedBag);
      ingest.setProjectAbbr(TestProject.PROJECT_ABBR.getValue());
      ingestService.ingest(ingest);

      // get the project after ingest
      var updatedProject = projectRepository.findById(TestProject.PROJECT_ABBR.getValue())
          .orElseThrow();
      var lastModifiedAfterIngest = updatedProject.getContentLastModified();

      Assertions.assertThat(lastModifiedAfterIngest)
          .isNotNull()
          .isAfter(lastModifiedBeforeIngest);
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
      Ingest ingest = new Ingest();
      ingest.setZippedBagItFolder(zippedBag);
      ingest.setProjectAbbr(TestProject.PROJECT_ABBR.getValue());
      ingestService.ingest(ingest);
    }

    @Test
    public void createsExpectedDigitalObject_withDatastreamsAndContentAndDublinCore(){

      // assert that the digital object was created
      Assertions.assertThat(digitalObjectRepository.findAll()).isNotEmpty();
      var datastreams = datastreamRepository.findAll();
      Assertions.assertThat(datastreams)
          .isNotEmpty()
          .hasSize(5);

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
    public void createsDigitalObjectWithExpectedChecksums(){

        var digitalObject = digitalObjectRepository.findById(TestDigitalObject.DIGITAL_OBJECT_ID.getValue())
            .orElseThrow( () -> new RuntimeException("Digital object not found"));

        Assertions.assertThat(digitalObject.getBaseMetadata().getMd5Checksum())
            .isEqualTo(TestDigitalObject.DIGITAL_OBJECT_MD5_CHECKSUM.getValue());

        Assertions.assertThat(digitalObject.getBaseMetadata().getSha512Checksum())
            .isEqualTo(TestDigitalObject.DIGITAL_OBJECT_SHA512_CHECKSUM.getValue());

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
        var bagEntity = bagEntityRepository.findById(TestIngestRecord.ID);
        Assertions.assertThat(bagEntity)
            .isPresent();
        var foundBagEntity = bagEntity.get();

        Assertions.assertThat(foundBagEntity.getId()).isEqualTo(TestIngestRecord.ID);
        Assertions.assertThat(foundBagEntity.getBagCreatedBy()).isEqualTo(TestBag.TestBagSipJson.CREATED_BY);
        Assertions.assertThat(foundBagEntity.getBagSchema()).isEqualTo(TestBag.TestBagSipJson.SCHEMA);
        Assertions.assertThat(foundBagEntity.getBagSource()).isEqualTo(TestBag.TestBagSipJson.SOURCE);
        Assertions.assertThat(foundBagEntity.getBagExternalDescription()).isEqualTo(TestBag.TestBagInfo.EXTERNAL_DESCRIPTION);
        Assertions.assertThat(foundBagEntity.getBaggingTimeStamp()).isEqualTo(TestBag.TestBagInfo.BAGGING_TIMESTAMP);
        Assertions.assertThat(foundBagEntity.getBagContactMail()).isEqualTo(TestBag.TestBagInfo.CONTACT_EMAIL);
        Assertions.assertThat(foundBagEntity.getBagPayloadOxum()).isEqualTo(TestBag.TestBagInfo.PAYLOAD_OXUM);

    }

    @Test
    public void ingestCreatesExpectedDatastreamCount(){

      var datastreams = datastreamRepository.findAll();
      Assertions.assertThat(datastreams)
          .isNotEmpty()
          .hasSize(5);

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

      // TODO all fields correct?

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

      Assertions.assertThat(foundDatastream.getBaseMetadata().getMd5Checksum())
          .isEqualTo(TestDatastream.METADATA_BASE_ENTITY.getMd5Checksum());

      Assertions.assertThat(foundDatastream.getBaseMetadata().getSha512Checksum())
          .isEqualTo(TestDatastream.METADATA_BASE_ENTITY.getSha512Checksum());


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

  }

  @Nested
  public class IngestTriggersExpectedEvents {

    @Test
    public void ingestShouldTriggerDigitalObjectCreatedEventExactlyOnce() throws IOException {

      bagFile = TestBag.loadFile();
      projectRepository.save(ProjectBuilder.builder().projectAbbr(TestProject.PROJECT_ABBR.getValue()).build());

      // ingest the bag
      byte[] zippedBag = ZipUtils.zipDir(bagFile);
      Ingest ingest = new Ingest();
      ingest.setZippedBagItFolder(zippedBag);
      ingest.setProjectAbbr(TestProject.PROJECT_ABBR.getValue());
      ingestService.ingest(ingest);


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
      Ingest ingest = new Ingest();
      ingest.setZippedBagItFolder(zippedBag);
      ingest.setProjectAbbr(TestProject.PROJECT_ABBR.getValue());

      Assertions.assertThatThrownBy(
          () -> ingestService.ingest(ingest)
      ).isInstanceOf(IngestObjectAlreadyExistsException.class);

    }


  }


  @Nested
  public class Export {

    @BeforeEach
    public void setup() throws IOException {
      bagFile = TestBag.loadFile();
      projectRepository.save(ProjectBuilder.builder().projectAbbr(TestProject.PROJECT_ABBR.getValue()).build());

      // ingest the bag
      byte[] zippedBag = ZipUtils.zipDir(bagFile);
      Ingest ingest = new Ingest();
      ingest.setZippedBagItFolder(zippedBag);
      ingest.setProjectAbbr(TestProject.PROJECT_ABBR.getValue());
      ingestService.ingest(ingest);

      // TODO move following assertion to own test
      Assertions.assertThat(
          datastreamContentRepository.exists(
              DatastreamId.builder().digitalObject(TestDigitalObject.DIGITAL_OBJECT_ID.getValue()).dsid("test.xml").build())
      ).isTrue();


    }

    @Test
    public void exportedBagHasExpectedStructure() throws IOException {
      // TODO rename?

      // create an output stream and check content


      try (
          var outputStream = new ByteArrayOutputStream()
          ) {
        ingestService.exportBag(TestDigitalObject.DIGITAL_OBJECT_ID.getValue(), outputStream);

        // read output stream as zip and check structure
        var zipBytes = outputStream.toByteArray();

        List<String> entryNames = new ArrayList<>();

        ZipUtils.walkZippedDir(zipBytes, (zipEntry, byteArrayOutputStream) -> {
          System.out.println("Found zip entry: " + zipEntry.getName());
          Assertions.assertThat(zipEntry.getName()).isNotBlank();
          Assertions.assertThat(byteArrayOutputStream.size()).isGreaterThan(0);
          entryNames.add(zipEntry.getName());

          // read content of some expected files
          // zipEntry is a path -> only take filename for switch
          // TODO extraction of filename is error prone and instransparent (maybe use Path class?)
          String fileName = zipEntry.getName().contains("/") ?
              zipEntry.getName().substring(zipEntry.getName().lastIndexOf("/") + 1) :
              zipEntry.getName();

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
              Assertions.assertThat(bagInfoTxtContent).contains(TestBag.TestBagInfo.BAGGING_TIME);
              Assertions.assertThat(bagInfoTxtContent).contains(TestBag.TestBagInfo.CONTACT_EMAIL);
              // TODO payload-oxum makes no sense here?
              Assertions.assertThat(bagInfoTxtContent).contains(TestBag.TestBagInfo.PAYLOAD_OXUM.toString());
            }
            case "sip.json" -> {
              String sipJsonContent = byteArrayOutputStream.toString();
              Assertions.assertThat(sipJsonContent).contains(TestBag.TestBagSipJson.CREATED_BY);
              Assertions.assertThat(sipJsonContent).contains(TestBag.TestBagSipJson.SCHEMA);
              Assertions.assertThat(sipJsonContent).contains(TestBag.TestBagSipJson.SOURCE);
              // TODO add more assertions?
            }
            case "manifest-md5.txt" -> {
              // TODO think about are those good assertions?
              String manifestMd5Content = byteArrayOutputStream.toString();
              // Assertions.assertThat(manifestMd5Content).contains(TestDigitalObject.DIGITAL_OBJECT_MD5_CHECKSUM.getValue());
              // Assertions.assertThat(manifestMd5Content).contains("data/meta/sip.json");
              Assertions.assertThat(manifestMd5Content).contains("data/content/DC.xml");
              Assertions.assertThat(manifestMd5Content).contains("140193d9633d8449ee1bff28030fe045");
            }
            case "manifest-sha512.txt" -> {
              // TODO think about are those good assertions?
              String manifestSha512Content = byteArrayOutputStream.toString();
              Assertions.assertThat(manifestSha512Content).contains("data/content/DC.xml");
              //Assertions.assertThat(manifestSha512Content).contains(TestDigitalObject.DIGITAL_OBJECT_SHA512_CHECKSUM.getValue());
              //Assertions.assertThat(manifestSha512Content).contains("data/meta/sip.json");
            }

            default -> {
              // do nothing
            }
          }
        });


        Assertions.assertThat(entryNames.size()).isEqualTo(10);

        Assertions.assertThat(entryNames).contains(
            String.format("%s/bagit.txt", TestDigitalObject.DIGITAL_OBJECT_ID.getValue()),
            String.format("%s/bag-info.txt", TestDigitalObject.DIGITAL_OBJECT_ID.getValue()),
            String.format("%s/manifest-md5.txt", TestDigitalObject.DIGITAL_OBJECT_ID.getValue()),
            String.format("%s/manifest-sha512.txt", TestDigitalObject.DIGITAL_OBJECT_ID.getValue()),
            String.format("%s/data/meta/sip.json", TestDigitalObject.DIGITAL_OBJECT_ID.getValue())
        );


      }


    }

  }


}
