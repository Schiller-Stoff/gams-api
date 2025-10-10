package org.zim.gamsapi.DigitalObject;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.auditing.AuditingHandler;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MultiValueMap;
import org.zim.gamsapi.Datastream.utils.interfaces.IDatastreamContentRepository;
import org.zim.gamsapi.Datastream.utils.interfaces.IDatastreamRepository;
import org.zim.gamsapi.DigitalObject.DublinCoreEntry.DublinCoreEntry;
import org.zim.gamsapi.DigitalObject.DublinCoreEntry.IDublinCoreEntryRepository;
import org.zim.gamsapi.DigitalObject.utils.exceptions.DigitalObjectNotFoundException;
import org.zim.gamsapi.DigitalObject.utils.interfaces.DigitalObjectListItemView;
import org.zim.gamsapi.DigitalObject.utils.interfaces.IDigitalObjectRepository;
import org.zim.gamsapi.DigitalObject.utils.interfaces.IDigitalObjectService;
import org.zim.gamsapi.DigitalObject.Ingest.Ingest;
import org.zim.gamsapi.DigitalObject.Ingest.IngestService;
import org.zim.gamsapi.DigitalObject.Ingest.utils.Bagit.BagFilePaths;
import org.zim.gamsapi.DigitalObject.Ingest.utils.ZipUtils;
import org.zim.gamsapi.IntegrationTest;
import org.zim.gamsapi.Project.Project;
import org.zim.gamsapi.Project.ProjectBuilder;
import org.zim.gamsapi.Project.exceptions.ProjectNotFoundException;
import org.zim.gamsapi.Project.interfaces.IProjectRepository;
import org.zim.gamsapi.System.dto.PagedResponse;
import org.zim.gamsapi.TestUtilities.*;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class DigitalObjectServiceIT extends IntegrationTest {

  @Autowired
  IDigitalObjectRepository digitalObjectRepository;

  @Autowired
  IDatastreamRepository datastreamRepository;

  @Autowired
  IDatastreamContentRepository datastreamContentRepository;

  @Autowired
  IProjectRepository projectRepository;

  @Autowired
  IDigitalObjectService digitalObjectService;

  @Autowired
  IDublinCoreEntryRepository dublinCoreEntryRepository;

  @Autowired
  IngestService ingestService;

  // Deactivates the auditing process.
  @MockitoBean
  private AuditingHandler auditingHandler;

  @Autowired
  private TestDataBuilder testDataBuilder;

  @Nested
  public class TestDataTests {

    private TestDataSet testDataSet;

    @BeforeEach
    public void setup(){
      testDataSet = testDataBuilder.buildTestDataSet();
    }

    @Nested
    public class Save {


      @Test
      public void successFullySavesSimpleDigitalObject() {
        // given
        DigitalObject digitalObject = TestDigitalObject.generate(
            testDataSet.project().getProjectAbbr(),
            testDataSet.project().getProjectAbbr() + ".randomid"
        );

        DigitalObject savedDigitalObject = digitalObjectService.save(digitalObject);

        // then
        Assertions.assertThat(savedDigitalObject).isNotNull();
        Assertions.assertThat(savedDigitalObject.getId()).isNotNull();
        Assertions.assertThat(savedDigitalObject.getProject()).isEqualTo(testDataSet.project());
        // considered equal because of same id
        Assertions.assertThat(savedDigitalObject).isEqualTo(digitalObject);

      }

    }

    @Nested
    public class FindAllByProjectAbbr {

      @Test
      public void returnsEmptyPageWhenNoDigitalObjectsExistForProject() {
        testDataBuilder.removeAllExceptProjects(testDataSet);
        PagedResponse<DigitalObjectListItemView> result = digitalObjectService.findAllByProjectAbbr(
            testDataSet.project().getProjectAbbr(),
            Optional.empty(),
            Pageable.unpaged()
        );
        Assertions.assertThat(result.getContent()).isEmpty();
      }

      @Test
      public void returnsPageOfDigitalObjectsWhenTheyExistForProject() {

        var result = digitalObjectService.findAllByProjectAbbr(
            testDataSet.project().getProjectAbbr(),
            Optional.empty(),
            Pageable.unpaged()
        );

        Assertions.assertThat(result.getContent()).isNotEmpty();
        Assertions.assertThat(result.getContent().get(0).getId()).isEqualTo(
            testDataSet.digitalObject().getId()
        );

      }

      @Test
      public void throwsExceptionWhenProjectDoesNotExist() {
        String projectAbbr = "nonExistentProject";
        Assertions.assertThatThrownBy(() -> digitalObjectService.findAllByProjectAbbr(projectAbbr, Optional.empty(), Pageable.unpaged()))
            .isInstanceOf(ProjectNotFoundException.class);
      }

      @Test
      public void findsDigitalObjectViaContainedInId(){

        digitalObjectService.findAllByProjectAbbr(
                testDataSet.project().getProjectAbbr(),
                testDataSet.digitalObject().getId(),
                PageRequest.of(0,100))
            .getContent()
            .forEach(digitalObject -> {
              Assertions.assertThat(digitalObject.getId()).isEqualTo(testDataSet.digitalObject().getId());
              Assertions.assertThat(digitalObject.getProject().getProjectAbbr()).isEqualTo(testDataSet.project().getProjectAbbr());
            });

      }


    }

    @Nested
    public class FindById {

      @Test
      public void returnsDigitalObjectWhenItExists() {
        DigitalObject result = digitalObjectService.findById(
            testDataSet.digitalObject().getId()
        );
        Assertions.assertThat(result).isEqualTo(testDataSet.digitalObject());
      }

      @Test
      public void throwsExceptionWhenDigitalObjectDoesNotExist() {
        String id = "nonExistentId";
        org.junit.jupiter.api.Assertions.assertThrows(
            DigitalObjectNotFoundException.class, () -> digitalObjectService.findById(id)
        );
      }

      @Test
      public void returnsDigitalObjectWithExpectedProperties(){


        DigitalObject foundObject = digitalObjectService.findById(testDataSet.digitalObject().getId());
        Assertions.assertThat(foundObject.getFunder()).isEqualTo(testDataSet.digitalObject().getFunder());
        Assertions.assertThat(foundObject.getId()).isEqualTo(testDataSet.digitalObject().getId());
        Assertions.assertThat(foundObject.getObjectType()).isEqualTo(testDataSet.digitalObject().getObjectType());
        Assertions.assertThat(foundObject.getPublisher()).isEqualTo(testDataSet.digitalObject().getPublisher());
        Assertions.assertThat(foundObject.getProject()).isEqualTo(testDataSet.digitalObject().getProject());
        Assertions.assertThat(foundObject.getBaseMetadata()).isEqualTo(testDataSet.digitalObject().getBaseMetadata());
        Assertions.assertThat(foundObject.getMainResource()).isEqualTo(testDataSet.digitalObject().getMainResource());
        // cannot be equal is being assigned by the database
        Assertions.assertThat(foundObject.getModified()).isNotEqualTo(testDataSet.digitalObject().getModified());
        Assertions.assertThat(foundObject.getCreated()).isNotEqualTo(testDataSet.digitalObject().getCreated());

      }

    }

    @Nested
    public class FindAllByProjectAbbrWithOptionalParameters {

      @Test
      public void returnsEmptyPageWhenNoDigitalObjectsExistForProject() {
        testDataBuilder.removeAllExceptProjects(testDataSet);
        PagedResponse<DigitalObjectListItemView> result = digitalObjectService.findAllByProjectAbbr(
            testDataSet.project().getProjectAbbr(), Optional.empty(), Pageable.unpaged()
        );
        Assertions.assertThat(result.getContent()).isEmpty();
      }

      @Test
      public void returnsPageOfDigitalObjectsWhenTheyExistForProject() {

        PagedResponse<DigitalObjectListItemView> result = digitalObjectService.findAllByProjectAbbr(
            testDataSet.project().getProjectAbbr(),
            Optional.of(testDataSet.digitalObject().getObjectType()),
            Pageable.unpaged()
        );

        Assertions.assertThat(result.getContent()).isNotEmpty();
        Assertions.assertThat(result.getContent().get(0).getId()).isEqualTo(
            testDataSet.digitalObject().getId()
        );

      }

      @Test
      public void throwsExceptionWhenProjectDoesNotExist() {
        String projectAbbr = "nonExistentProject";

        Assertions.assertThatThrownBy(() -> digitalObjectService.findAllByProjectAbbr(projectAbbr, Optional.empty(), Pageable.unpaged()))
            .isInstanceOf(ProjectNotFoundException.class);
      }
    }

    @Nested
    public class Delete {

      @Test
      public void deletesDigitalObject() {
        digitalObjectService.delete(testDataSet.digitalObject());
        Assertions.assertThatThrownBy(() -> digitalObjectService.findById(
                testDataSet.digitalObject().getId())
            )
            .isInstanceOf(DigitalObjectNotFoundException.class);
      }

      @Test
      public void deletesChildDatastreamsWithFileContent() {
        digitalObjectService.delete(testDataSet.digitalObject());
        Assertions.assertThat(datastreamRepository.existsById(testDataSet.mainDatastream().deriveDatastreamId())).isFalse();
        Assertions.assertThat(datastreamContentRepository.exists(testDataSet.mainDatastream().deriveDatastreamId())).isFalse();
      }

      @Test
      public void deletesReferencedDublinCoreEntries(){
        digitalObjectService.delete(
            testDataSet.digitalObject()
        );
        Assertions.assertThat(
            dublinCoreEntryRepository.existsById(testDataSet.dublinCoreEntry().getId())
        ).isFalse();

      }

      @Test
      public void projectContentIsUpdatedWhenDigitalObjectIsDeleted() {

        Date projectContentLastModifiedBeforeDelete = testDataSet.project().getContentLastModified();

        // this is a side effect of the delete operation, but we want to ensure that it works
        digitalObjectService.delete(testDataSet.digitalObject());

        var updatedProject = projectRepository.findById(testDataSet.project().getProjectAbbr())
            .orElseThrow(() ->  new ProjectNotFoundException(testDataSet.project().getProjectAbbr()));

        Date projectContentLastModifedAfterDelete = updatedProject.getContentLastModified();

        Assertions.assertThat(projectContentLastModifedAfterDelete)
            .isNotNull()
            .isAfter(projectContentLastModifiedBeforeDelete);

      }

    }


    @Nested
    public class FindDigitalObjectCompactDTOById {

      @Test
      @Transactional
      public void containsExpectedDublinCoreEntry(){

        var foundDigitalObject = digitalObjectService.findDigitalObjectCompactDTOById(
            testDataSet.digitalObject().getId()
        );

        Assertions.assertThat(foundDigitalObject)
            .isNotNull();

        var entries = foundDigitalObject.getDublinCore();

        Assertions.assertThat(entries)
            .isNotEmpty()
            .hasSize(1)
            .containsKey(testDataSet.dublinCoreEntry().getName());

        var testElementEntries = entries.get(testDataSet.dublinCoreEntry().getName());
        Assertions.assertThat(testElementEntries)
            .anySatisfy(entry -> {
              Assertions.assertThat(entry.language()).isEqualTo(testDataSet.dublinCoreEntry().getLanguage());
              Assertions.assertThat(entry.value()).isEqualTo(testDataSet.dublinCoreEntry().getValue());
            });
      }

    }


    @Nested
    public class DublinCoreFulltextSearch {

      Project additionalProject;

      @BeforeEach
      public void setup(){

        // 1 object belongs to a different project
        additionalProject =  testDataBuilder.addRandomProject(testDataSet);


        List<DigitalObject> digitalObjects = List.of(
            TestDigitalObject.generate("test", "test.foo"),
            TestDigitalObject.generate("test", "test.bar"),
            TestDigitalObject.generate("test", "test.baz"),
            // belongs to a different project
            TestDigitalObject.generate(additionalProject.getProjectAbbr(), additionalProject.getProjectAbbr() + ".peter")
        );

        digitalObjectRepository.saveAll(digitalObjects);

        List<DublinCoreEntry> dublinCoreEntries = List.of(
            TestDublinCoreEntry.generate(digitalObjects.get(0).getId()),
            TestDublinCoreEntry.generate(digitalObjects.get(1).getId()),
            TestDublinCoreEntry.generate(digitalObjects.get(2).getId()),
            TestDublinCoreEntry.generate(additionalProject.getProjectAbbr(), digitalObjects.get(3).getId())
        );

        dublinCoreEntryRepository.saveAll(dublinCoreEntries);

      }


      @Test
      public void findsExpectedObjectCount(){

        // arbitrary fulltext-search query (based on test data)
        final String TEST_SEARCH_VALUE = testDataSet.dublinCoreEntry().getValue().substring(0, 3);

        var foundDigitalObjects = digitalObjectService.searchByDCFulltext(
            Set.of(testDataSet.project().getProjectAbbr()),
            // empty -> runs fulltext across all dc fields
            Set.of(),
            TEST_SEARCH_VALUE,
            Pageable.unpaged()
        );

        long EXPECTED_OBJECT_COUNT = 4; // // only three objects assigned to this project (but one is already in test data set)

        Assertions.assertThat(foundDigitalObjects.getContent())
            .isNotEmpty()
        ;
        Assertions.assertThat(
            foundDigitalObjects.getPagination().getTotalElements()
        ).isEqualTo(EXPECTED_OBJECT_COUNT);
      }

      @Test
      public void findsNothingWhenNotInDCField(){

        // arbitrary fulltext-search query (based on test data)
        final String TEST_SEARCH_VALUE = TestDublinCoreEntry.VALUE.getValue().substring(0, 3);
        final Set<String> TEST_DC_FIELDS = Set.of("type"); // should not be found

        var foundDigitalObjects = digitalObjectService.searchByDCFulltext(
            // only three objects assigned to this project
            Set.of(testDataSet.project().getProjectAbbr()),
            TEST_DC_FIELDS,
            TEST_SEARCH_VALUE,
            Pageable.unpaged()
        );
        Assertions.assertThat(foundDigitalObjects.getContent())
            .isEmpty();
        Assertions.assertThat(foundDigitalObjects.getPagination().getTotalElements()).isEqualTo(0);

      }

    }


    @Nested
    public class SearchByDublinCoreCriteria {

      @Test
      @Transactional
      public void findsExpectedDigitalObject(){

        var dcFilters = Map.of(
            testDataSet.dublinCoreEntry().getName(),
            List.of(testDataSet.dublinCoreEntry().getValue())
        );

        var foundObjects = digitalObjectService.searchDigitalObjectsByDublinCoreCriteria(
            MultiValueMap.fromMultiValue(dcFilters),
            Set.of(testDataSet.project().getProjectAbbr()),
            DigitalObjectDublinCoreSpecification.SearchMode.FULLTEXT,
            PageRequest.of(0,100)
        );

        Assertions.assertThat(foundObjects.getContent())
            .isNotNull()
            .isNotEmpty()
            .hasSize(1)
            .allSatisfy(digitalObject -> {
              Assertions.assertThat(digitalObject.getId()).isEqualTo(testDataSet.digitalObject().getId());
              Assertions.assertThat(digitalObject.getProjectAbbr()).isEqualTo(testDataSet.project().getProjectAbbr());
            });

      }
    }

    @Nested
    public class FindAllIdsByProjectAbbr {

      @Test
      public void returnsExpectedDigitalObjectIds(){

        // adding two additional digital objects to the test data set
        DigitalObject digitalObject1 = testDataBuilder.addRandomObject(testDataSet);
        DigitalObject digitalObject2 = testDataBuilder.addRandomObject(testDataSet);
        final int EXPECTED_OBJECT_COUNT = 3;

        var paginatedIds = digitalObjectService.findAllIdsByProjectAbbr(
            testDataSet.project().getProjectAbbr(), PageRequest.of(0,1000)
        );

        Assertions
            .assertThat(paginatedIds)
            .isNotNull();

        Assertions.assertThat(paginatedIds.getContent())
            .hasSize(EXPECTED_OBJECT_COUNT)
            .contains(digitalObject1.getId(), digitalObject2.getId());

      }


    }


  }

  @Nested
  public class IngestTests {

    File bagFile;

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
    public void exportedBagHasExpectedStructure() throws IOException {

      try (
          var outputStream = new ByteArrayOutputStream()
      ) {
        digitalObjectService.exportAsBag(TestDigitalObject.DIGITAL_OBJECT_ID.getValue(), outputStream);

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
              Assertions.assertThat(bagInfoTxtContent).contains(TestBag.TestBagInfo.BAGGING_TIME);
              Assertions.assertThat(bagInfoTxtContent).contains(TestBag.TestBagInfo.CONTACT_EMAIL);
              Assertions.assertThat(bagInfoTxtContent).contains(TestBag.TestBagInfo.PAYLOAD_OXUM.toString());
            }
            case "sip.json" -> {
              String sipJsonContent = byteArrayOutputStream.toString();
              Assertions.assertThat(sipJsonContent).contains(TestBag.TestBagSipJson.REC_ID);
              Assertions.assertThat(sipJsonContent).contains(TestBag.TestBagSipJson.PROJECT);
              Assertions.assertThat(sipJsonContent).contains(TestBag.TestBagSipJson.CREATED_BY);
              Assertions.assertThat(sipJsonContent).contains(TestBag.TestBagSipJson.SCHEMA);
              Assertions.assertThat(sipJsonContent).contains(TestBag.TestBagSipJson.SOURCE);
              Assertions.assertThat(sipJsonContent).contains(TestBag.TestBagSipJson.CREATOR);
              Assertions.assertThat(sipJsonContent).contains(TestBag.TestBagSipJson.TITLE);
              Assertions.assertThat(sipJsonContent).contains(TestBag.TestBagSipJson.DESCRIPTION);
              Assertions.assertThat(sipJsonContent).contains(TestBag.TestBagSipJson.PUBLISHER);
              Assertions.assertThat(sipJsonContent).contains(TestBag.TestBagSipJson.FUNDER);
              Assertions.assertThat(sipJsonContent).contains(TestBag.TestBagSipJson.MAIN_RESOURCE);

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

              // TODO assert other contentFiles?


            }
            case "manifest-md5.txt" -> {
              // TODO think about are those good assertions?
              String manifestMd5Content = byteArrayOutputStream.toString();
              Assertions.assertThat(manifestMd5Content).contains(BagFilePaths.BAG_SIP_JSON.name);
              Assertions.assertThat(manifestMd5Content).contains(BagFilePaths.DUBLIN_CORE_XML.name);
              Assertions.assertThat(manifestMd5Content).contains("140193d9633d8449ee1bff28030fe045");
            }
            case "manifest-sha512.txt" -> {
              // TODO think about are those good assertions?
              String manifestSha512Content = byteArrayOutputStream.toString();
              Assertions.assertThat(manifestSha512Content).contains(BagFilePaths.DUBLIN_CORE_XML.name);
              Assertions.assertThat(manifestSha512Content).contains(BagFilePaths.BAG_SIP_JSON.name);
            }

            default -> {
              // do nothing
            }
          }
        });


        Assertions.assertThat(entryNames.size()).isEqualTo(10);

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
            String.format("%s/data/content/search.json", TestDigitalObject.DIGITAL_OBJECT_ID.getValue())
        );



      }


    }

  }

}
