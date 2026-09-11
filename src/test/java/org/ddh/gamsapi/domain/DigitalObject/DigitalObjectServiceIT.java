package org.ddh.gamsapi.domain.DigitalObject;

import org.assertj.core.api.Assertions;
import org.ddh.gamsapi.IntegrationTest;
import org.ddh.gamsapi.TestUtilities.*;
import org.ddh.gamsapi.domain.ArchivalRecord.IArchivalRecordRepository;
import org.ddh.gamsapi.domain.Datastream.Datastream;
import org.ddh.gamsapi.domain.Datastream.DatastreamId;
import org.ddh.gamsapi.domain.Datastream.utils.GAMSDsid;
import org.ddh.gamsapi.domain.Datastream.utils.interfaces.IDatastreamContentRepository;
import org.ddh.gamsapi.domain.Datastream.utils.interfaces.IDatastreamRepository;
import org.ddh.gamsapi.domain.DigitalObject.DublinCoreEntry.DublinCoreEntry;
import org.ddh.gamsapi.domain.DigitalObject.DublinCoreEntry.IDublinCoreEntryRepository;
import org.ddh.gamsapi.domain.DigitalObject.utils.dto.DigitalObjectCreateDto;
import org.ddh.gamsapi.domain.DigitalObject.utils.dto.DigitalObjectUpdateDto;
import org.ddh.gamsapi.domain.DigitalObject.utils.exceptions.DigitalObjectAlreadyExistsException;
import org.ddh.gamsapi.domain.DigitalObject.utils.exceptions.DigitalObjectNotFoundException;
import org.ddh.gamsapi.domain.DigitalObject.utils.exceptions.DigitalObjectValidationException;
import org.ddh.gamsapi.domain.DigitalObject.utils.interfaces.DigitalObjectListItemView;
import org.ddh.gamsapi.domain.DigitalObject.utils.interfaces.IDigitalObjectRepository;
import org.ddh.gamsapi.domain.DigitalObject.utils.interfaces.IDigitalObjectService;
import org.ddh.gamsapi.domain.Project.Project;
import org.ddh.gamsapi.domain.Project.exceptions.ProjectNotFoundException;
import org.ddh.gamsapi.domain.Project.interfaces.IProjectRepository;
import org.ddh.gamsapi.infrastructure.System.dto.PagedResponse;
import org.ddh.gamsapi.infrastructure.System.security.IUserPrincipalAuditorMapping;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.auditing.AuditingHandler;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DigitalObjectServiceIT extends IntegrationTest {

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
  IArchivalRecordRepository archivalRecordRepository;


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
  void setup(){
    testDataSet = testDataBuilder.buildTestDataSet();
    // needed when changing datastreams
    Mockito.when(userPrincipalAuditorMapping.getCurrentAuditor())
        .thenReturn(Optional.of(TestUser.USERNAME.getValue()));
  }

  @Nested
  class Save {


    @Test
    void successFullySavesSimpleDigitalObject() {
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

      // assert that ingest property is auto set to false
      Assertions.assertThat(savedDigitalObject.isIngested()).isFalse();

      // considered equal because of same id
      Assertions.assertThat(savedDigitalObject).isEqualTo(digitalObject);

    }

    @Test
    void savingExistingObjectChangesCreationAfterModified(){

      var savedObject = digitalObjectRepository.findById(testDataSet.digitalObject().getId())
          .orElseThrow();

      var oldModificationDate = testDataSet.digitalObject().getModified();
      Assertions.assertThat(savedObject.isModifiedAfterCreation())
          .isFalse();

      var oldModifiedBy =  testDataSet.digitalObject().getModifiedBy();
      Assertions.assertThat(oldModifiedBy).isNotEqualTo(TestUser.USERNAME.getValue());

      // small delay to ensure timestamp difference
      try { Thread.sleep(50); } catch (InterruptedException _) {
        // ignored
      }

      // change something
      savedObject.setObjectType("DEMO VALUE");
      savedObject = digitalObjectService.save(savedObject);
      Assertions.assertThat(savedObject.isModifiedAfterCreation())
          .isTrue();

      var newModificationDate = savedObject.getModified();
      Assertions.assertThat(oldModificationDate).isBefore(newModificationDate);

      // modified by
      Assertions.assertThat(savedObject.getModifiedBy()).isEqualTo(TestUser.USERNAME.getValue());

    }

  }

  @Nested
  class FindAllByProjectAbbr {

    @Test
    void returnsEmptyPageWhenNoDigitalObjectsExistForProject() {
      testDataBuilder.removeAllExceptProjects(testDataSet);
      PagedResponse<DigitalObjectListItemView> result = digitalObjectService.findAllByProjectAbbr(
          testDataSet.project().getProjectAbbr(),
          Optional.empty(),
          Pageable.unpaged()
      );
      Assertions.assertThat(result.getContent()).isEmpty();
    }

    @Test
    void returnsPageOfDigitalObjectsWhenTheyExistForProject() {

      var result = digitalObjectService.findAllByProjectAbbr(
          testDataSet.project().getProjectAbbr(),
          Optional.empty(),
          Pageable.unpaged()
      );

      Assertions.assertThat(result.getContent()).isNotEmpty();
      Assertions.assertThat(result.getContent().getFirst().getId()).isEqualTo(
          testDataSet.digitalObject().getId()
      );

    }

    @Test
    void throwsExceptionWhenProjectDoesNotExist() {
      String projectAbbr = "nonExistentProject";
      Assertions.assertThatThrownBy(() -> digitalObjectService.findAllByProjectAbbr(projectAbbr, Optional.empty(), Pageable.unpaged()))
          .isInstanceOf(ProjectNotFoundException.class);
    }

    @Test
    void findsDigitalObjectExactId(){

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

    @Test
    void findsDigitalObjectStartsWithId(){

      String idStartsWith = testDataSet.digitalObject().getId().substring(0,5);

      digitalObjectService.findAllByProjectAbbr(
              testDataSet.project().getProjectAbbr(),
              idStartsWith,
              PageRequest.of(0,100))
          .getContent()
          .forEach(digitalObject -> {
            Assertions.assertThat(digitalObject.getId()).startsWith(idStartsWith);
            Assertions.assertThat(digitalObject.getProject().getProjectAbbr()).isEqualTo(testDataSet.project().getProjectAbbr());
          });

    }


  }

  @Nested
  class FindById {

    @Test
    void returnsDigitalObjectWhenItExists() {
      DigitalObject result = digitalObjectService.findById(
          testDataSet.digitalObject().getId()
      );
      Assertions.assertThat(result).isEqualTo(testDataSet.digitalObject());
    }

    @Test
    void throwsExceptionWhenDigitalObjectDoesNotExist() {
      String id = "nonExistentId";
      org.junit.jupiter.api.Assertions.assertThrows(
          DigitalObjectNotFoundException.class, () -> digitalObjectService.findById(id)
      );
    }

    @Test
    void returnsDigitalObjectWithExpectedProperties(){
      DigitalObject foundObject = digitalObjectService.findById(testDataSet.digitalObject().getId());
      Assertions.assertThat(foundObject.getFunder()).isEqualTo(testDataSet.digitalObject().getFunder());
      Assertions.assertThat(foundObject.getId()).isEqualTo(testDataSet.digitalObject().getId());
      Assertions.assertThat(foundObject.getObjectType()).isEqualTo(testDataSet.digitalObject().getObjectType());
      Assertions.assertThat(foundObject.getPublisher()).isEqualTo(testDataSet.digitalObject().getPublisher());
      Assertions.assertThat(foundObject.getProject()).isEqualTo(testDataSet.digitalObject().getProject());
      Assertions.assertThat(foundObject.getBaseMetadata()).isEqualTo(testDataSet.digitalObject().getBaseMetadata());
      Assertions.assertThat(foundObject.getMainResource()).isEqualTo(testDataSet.digitalObject().getMainResource());
      Assertions.assertThat(foundObject.getModified()).isEqualTo(testDataSet.digitalObject().getModified());
      Assertions.assertThat(foundObject.getCreated()).isEqualTo(testDataSet.digitalObject().getCreated());

      Assertions.assertThat(
          foundObject.getTags()
      ).containsAll(
          testDataSet.digitalObject().getTags()
      );

    }

  }

  @Nested
  class FindAllByProjectAbbrWithOptionalParameters {

    @Test
    void returnsEmptyPageWhenNoDigitalObjectsExistForProject() {
      testDataBuilder.removeAllExceptProjects(testDataSet);
      PagedResponse<DigitalObjectListItemView> result = digitalObjectService.findAllByProjectAbbr(
          testDataSet.project().getProjectAbbr(), Optional.empty(), Pageable.unpaged()
      );
      Assertions.assertThat(result.getContent()).isEmpty();
    }

    @Test
    void returnsPageOfDigitalObjectsWhenTheyExistForProject() {

      PagedResponse<DigitalObjectListItemView> result = digitalObjectService.findAllByProjectAbbr(
          testDataSet.project().getProjectAbbr(),
          Optional.of(testDataSet.digitalObject().getObjectType()),
          Pageable.unpaged()
      );

      Assertions.assertThat(result.getContent()).isNotEmpty();
      Assertions.assertThat(result.getContent().getFirst().getId()).isEqualTo(
          testDataSet.digitalObject().getId()
      );

    }

    @Test
    void throwsExceptionWhenProjectDoesNotExist() {
      String projectAbbr = "nonExistentProject";

      Assertions.assertThatThrownBy(() -> digitalObjectService.findAllByProjectAbbr(projectAbbr, Optional.empty(), Pageable.unpaged()))
          .isInstanceOf(ProjectNotFoundException.class);
    }
  }

  @Nested
  class Delete {

    @Test
    void successfullyDeletesDigitalObjectAlthoughArchivalRecordExists(){
      // verify test archival record exists
      Assertions.assertThat(
          archivalRecordRepository.existsByDigitalObjectId(testDataSet.digitalObject().getId())
          ).isTrue();

      digitalObjectService.delete(testDataSet.digitalObject());

      // object is deleted
      Assertions.assertThat(
          digitalObjectRepository.existsById(testDataSet.digitalObject().getId())
      ).isFalse();

      // but archival record still exists
      Assertions.assertThat(
          archivalRecordRepository.existsById(testDataSet.archivalRecord().getPid())
      ).isTrue();

      // check if reference is set to null as expected
      var foundArchivalRecord = archivalRecordRepository.findById(testDataSet.archivalRecord().getPid())
          .orElseThrow();
      Assertions.assertThat(foundArchivalRecord.getDigitalObject())
          .isNull();



    }

    @Test
    void deletesDigitalObject() {
      // test archival record must be deleted - otherwise error
      archivalRecordRepository.delete(testDataSet.archivalRecord());
      digitalObjectService.delete(testDataSet.digitalObject());
      Assertions.assertThatThrownBy(() -> digitalObjectService.findById(
              testDataSet.digitalObject().getId())
          )
          .isInstanceOf(DigitalObjectNotFoundException.class);
    }

    @Test
    void deletesChildDatastreamsWithFileContent() {
      // remove test archival record
      archivalRecordRepository.delete(testDataSet.archivalRecord());
      digitalObjectService.delete(testDataSet.digitalObject());
      Assertions.assertThat(datastreamRepository.existsById(testDataSet.mainDatastream().deriveDatastreamId())).isFalse();
      Assertions.assertThat(datastreamContentRepository.exists(testDataSet.mainDatastream().deriveDatastreamId())).isFalse();
    }

    @Test
    void deletesReferencedDublinCoreEntries(){
      // remove test archival record
      archivalRecordRepository.delete(testDataSet.archivalRecord());
      digitalObjectService.delete(
          testDataSet.digitalObject()
      );
      Assertions.assertThat(
          dublinCoreEntryRepository.existsById(testDataSet.dublinCoreEntry().getId())
      ).isFalse();

    }

    @Test
    void projectContentIsUpdatedWhenDigitalObjectIsDeleted() {

      Instant projectContentLastModifiedBeforeDelete = testDataSet.project().getModified();

      // remove test archival record
      archivalRecordRepository.delete(testDataSet.archivalRecord());
      digitalObjectService.delete(testDataSet.digitalObject());

      var updatedProject = projectRepository.findById(testDataSet.project().getProjectAbbr())
          .orElseThrow(() ->  new ProjectNotFoundException(testDataSet.project().getProjectAbbr()));

      Instant projectContentLastModifiedAfterDelete = updatedProject.getModified();

      Assertions.assertThat(projectContentLastModifiedAfterDelete)
          .isNotNull()
          .isAfter(projectContentLastModifiedBeforeDelete);

    }

  }


  @Nested
  class FindDigitalObjectCompactDTOById {

    @Test
    @Transactional
    void containsExpectedDublinCoreEntry(){

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
  class DublinCoreFulltextSearch {

    Project additionalProject;

    @BeforeEach
    void setup(){

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

  }

  @Nested
  class FindAllIdsByProjectAbbr {

    @Test
    void returnsExpectedDigitalObjectIds(){

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

  @Nested
  class FindAllByProjectAndTags {

    @Test
    void returnsDigitalObjectsWithExpectedTags(){

      var TAG_TO_FIND = TestDigitalObject.getTags();

      // adding two additional digital objects to the test data set
      var foundObjects = digitalObjectService.findAllByProjectAndTags(
          testDataSet.project().getProjectAbbr(),
          TAG_TO_FIND,
          PageRequest.of(0,100)
      );

      Assertions.assertThat(foundObjects.getPagination().getTotalElements())
          .isGreaterThan(0);

      var firstFoundObject = foundObjects.getContent().getFirst();

      Assertions.assertThat(firstFoundObject.getTags())
          .isNotNull()
          .isNotEmpty()
          .containsAll(TAG_TO_FIND);

    }

  }

  @Nested
  class FindDistinctTagsByProject {

    @Test
    void returnsDistinctTagsForProject(){

      // adding two additional digital objects to the test data set
      testDataBuilder.addRandomObject(testDataSet);
      testDataBuilder.addRandomObject(testDataSet);

      var distinctTags = digitalObjectService.findDistinctTagsByProject(
          testDataSet.project().getProjectAbbr()
      );

      Assertions.assertThat(distinctTags)
          .isNotNull()
          .isNotEmpty()
          .containsAll(TestDigitalObject.getTags());

    }

  }

  @Nested
  class CreateDigitalObject {

    private DigitalObjectCreateDto buildValidDto() {
      var dto = new DigitalObjectCreateDto();
      dto.setIdSuffix("democreate");
      dto.setTitle("Created Title");
      dto.setCreator("Created Creator");
      dto.setRights("CC BY 4.0");
      dto.setPublisher("Created Publisher");
      dto.setDescription("Created Description");
      dto.setObjectType("TEI");
      dto.setFunder("Created Funder");
      return dto;
    }

    @Test
    void createsDigitalObjectWithExpectedId() {
      var dto = buildValidDto();
      String expectedId = testDataSet.project().getProjectAbbr() + "." + dto.getIdSuffix();

      DigitalObject result = digitalObjectService.create(
          testDataSet.project().getProjectAbbr(), dto
      );

      Assertions.assertThat(result).isNotNull();
      Assertions.assertThat(result.getId()).isEqualTo(expectedId);
    }

    @Test
    void persistsDigitalObjectInDatabase() {
      var dto = buildValidDto();
      String expectedId = testDataSet.project().getProjectAbbr() + "." + dto.getIdSuffix();

      digitalObjectService.create(testDataSet.project().getProjectAbbr(), dto);

      var persisted = digitalObjectRepository.findById(expectedId);
      Assertions.assertThat(persisted).isPresent();
    }

    @Test
    void createdObjectHasExpectedMetadata() {
      var dto = buildValidDto();

      DigitalObject result = digitalObjectService.create(
          testDataSet.project().getProjectAbbr(), dto
      );

      Assertions.assertThat(result.getBaseMetadata().getTitle()).isEqualTo(dto.getTitle());
      Assertions.assertThat(result.getBaseMetadata().getCreator()).isEqualTo(dto.getCreator());
      Assertions.assertThat(result.getBaseMetadata().getRights()).isEqualTo(dto.getRights());
      Assertions.assertThat(result.getBaseMetadata().getDescription()).isEqualTo(dto.getDescription());
      Assertions.assertThat(result.getPublisher()).isEqualTo(dto.getPublisher());
      Assertions.assertThat(result.getFunder()).isEqualTo(dto.getFunder());
      Assertions.assertThat(result.getObjectType()).isEqualTo(dto.getObjectType());
    }

    @Test
    void createdObjectBelongsToExpectedProject() {
      var dto = buildValidDto();

      DigitalObject result = digitalObjectService.create(
          testDataSet.project().getProjectAbbr(), dto
      );

      Assertions.assertThat(result.getProject().getProjectAbbr())
          .isEqualTo(testDataSet.project().getProjectAbbr());
    }

    @Test
    void createsTimestamps() {
      var dto = buildValidDto();

      DigitalObject result = digitalObjectService.create(
          testDataSet.project().getProjectAbbr(), dto
      );

      // re-fetch to get DB-generated timestamps
      var persisted = digitalObjectRepository.findById(result.getId()).orElseThrow();
      Assertions.assertThat(persisted.getCreated()).isNotNull();
      // modified may be null or equal to created depending on Hibernate behavior
    }

    // --- Dublin Core entries ---

    @Test
    void createsDublinCoreEntries() {
      var dto = buildValidDto();
      String expectedId = testDataSet.project().getProjectAbbr() + "." + dto.getIdSuffix();

      digitalObjectService.create(testDataSet.project().getProjectAbbr(), dto);

      var dcEntries = dublinCoreEntryRepository.findByDigitalObjectId(expectedId);
      // Should have at least title, creator, rights, publisher (+ optional description)
      Assertions.assertThat(dcEntries)
          .isNotEmpty()
          .hasSizeGreaterThanOrEqualTo(4);
    }

    @Test
    void createsDublinCoreEntryForTitle() {
      var dto = buildValidDto();
      String expectedId = testDataSet.project().getProjectAbbr() + "." + dto.getIdSuffix();

      digitalObjectService.create(testDataSet.project().getProjectAbbr(), dto);

      var dcEntries = dublinCoreEntryRepository.findByDigitalObjectId(expectedId);
      Assertions.assertThat(dcEntries)
          .anySatisfy(entry -> {
            Assertions.assertThat(entry.getName()).isEqualTo("title");
            Assertions.assertThat(entry.getValue()).isEqualTo(dto.getTitle());
          });
    }

    @Test
    void createsDublinCoreEntryForDescription() {
      var dto = buildValidDto();
      String expectedId = testDataSet.project().getProjectAbbr() + "." + dto.getIdSuffix();

      digitalObjectService.create(testDataSet.project().getProjectAbbr(), dto);

      var dcEntries = dublinCoreEntryRepository.findByDigitalObjectId(expectedId);
      Assertions.assertThat(dcEntries)
          .anySatisfy(entry -> {
            Assertions.assertThat(entry.getName()).isEqualTo("description");
            Assertions.assertThat(entry.getValue()).isEqualTo(dto.getDescription());
          });
    }

    @Test
    void skipsDublinCoreDescriptionWhenEmpty() {
      var dto = buildValidDto();
      dto.setDescription(null);
      String expectedId = testDataSet.project().getProjectAbbr() + "." + dto.getIdSuffix();

      digitalObjectService.create(testDataSet.project().getProjectAbbr(), dto);

      var dcEntries = dublinCoreEntryRepository.findByDigitalObjectId(expectedId);
      Assertions.assertThat(dcEntries)
          .noneSatisfy(entry ->
              Assertions.assertThat(entry.getName()).isEqualTo("description")
          );
    }

    // --- DC.xml datastream ---

    @Test
    void createsDcXmlDatastream() {
      var dto = buildValidDto();
      String expectedId = testDataSet.project().getProjectAbbr() + "." + dto.getIdSuffix();

      digitalObjectService.create(testDataSet.project().getProjectAbbr(), dto);

      DatastreamId dcDsId = new DatastreamId(GAMSDsid.DC.getValue(), expectedId);
      Assertions.assertThat(datastreamRepository.existsById(dcDsId)).isTrue();
    }

    @Test
    void dcXmlDatastreamHasExpectedMimeType() {
      var dto = buildValidDto();
      String expectedId = testDataSet.project().getProjectAbbr() + "." + dto.getIdSuffix();

      digitalObjectService.create(testDataSet.project().getProjectAbbr(), dto);

      DatastreamId dcDsId = new DatastreamId(GAMSDsid.DC.getValue(), expectedId);
      Datastream dcDs = datastreamRepository.findById(dcDsId).orElseThrow();
      Assertions.assertThat(dcDs.getMimeType()).isEqualTo("application/xml");
    }

    @Test
    void dcXmlDatastreamHasChecksums() {
      var dto = buildValidDto();
      String expectedId = testDataSet.project().getProjectAbbr() + "." + dto.getIdSuffix();

      digitalObjectService.create(testDataSet.project().getProjectAbbr(), dto);

      DatastreamId dcDsId = new DatastreamId(GAMSDsid.DC.getValue(), expectedId);
      Datastream dcDs = datastreamRepository.findById(dcDsId).orElseThrow();
      Assertions.assertThat(dcDs.getMd5Checksum()).isNotEmpty();
      Assertions.assertThat(dcDs.getSha512Checksum()).isNotEmpty();
    }

    @Test
    void dcXmlDatastreamFileExistsOnDisk() {
      var dto = buildValidDto();
      String expectedId = testDataSet.project().getProjectAbbr() + "." + dto.getIdSuffix();

      digitalObjectService.create(testDataSet.project().getProjectAbbr(), dto);

      DatastreamId dcDsId = new DatastreamId(GAMSDsid.DC.getValue(), expectedId);
      Assertions.assertThat(datastreamContentRepository.exists(dcDsId)).isTrue();
    }

    @Test
    void dcXmlDatastreamHasPositiveSize() {
      var dto = buildValidDto();
      String expectedId = testDataSet.project().getProjectAbbr() + "." + dto.getIdSuffix();

      digitalObjectService.create(testDataSet.project().getProjectAbbr(), dto);

      DatastreamId dcDsId = new DatastreamId(GAMSDsid.DC.getValue(), expectedId);
      Datastream dcDs = datastreamRepository.findById(dcDsId).orElseThrow();
      Assertions.assertThat(dcDs.getSize()).isGreaterThan(0);
    }

    // --- Validation / error cases ---

    @Test
    void throwsWhenProjectDoesNotExist() {
      var dto = buildValidDto();

      Assertions.assertThatThrownBy(
          () -> digitalObjectService.create("nonexistent", dto)
      ).isInstanceOf(ProjectNotFoundException.class);
    }

    @Test
    void throwsWhenObjectAlreadyExists() {
      // The testDataSet already has a digital object with id "test.test"
      var dto = buildValidDto();
      // Use the existing object's id suffix
      String existingIdSuffix = testDataSet.digitalObject().getId()
          .replace(testDataSet.project().getProjectAbbr() + ".", "");
      dto.setIdSuffix(existingIdSuffix);

      Assertions.assertThatThrownBy(
          () -> digitalObjectService.create(
              testDataSet.project().getProjectAbbr(), dto
          )
      ).isInstanceOf(DigitalObjectAlreadyExistsException.class);
    }

    @Test
    void doesNotPersistObjectWhenDuplicateIdDetected() {
      var dto = buildValidDto();
      String existingIdSuffix = testDataSet.digitalObject().getId()
          .replace(testDataSet.project().getProjectAbbr() + ".", "");
      dto.setIdSuffix(existingIdSuffix);

      Assertions.assertThatThrownBy(
          () -> digitalObjectService.create(
              testDataSet.project().getProjectAbbr(), dto
          )
      ).isInstanceOf(DigitalObjectAlreadyExistsException.class);

      // Verify original object is unchanged
      var original = digitalObjectRepository.findById(
          testDataSet.digitalObject().getId()
      ).orElseThrow();
      Assertions.assertThat(original.getBaseMetadata().getTitle())
          .isEqualTo(testDataSet.digitalObject().getBaseMetadata().getTitle());
    }

    // --- Optional fields ---

    @Test
    void createsObjectWithNullDescription() {
      var dto = buildValidDto();
      dto.setDescription(null);

      DigitalObject result = digitalObjectService.create(
          testDataSet.project().getProjectAbbr(), dto
      );

      Assertions.assertThat(result.getBaseMetadata().getDescription()).isNull();
    }

    @Test
    void createsObjectWithNullFunder() {
      var dto = buildValidDto();
      dto.setFunder(null);

      DigitalObject result = digitalObjectService.create(
          testDataSet.project().getProjectAbbr(), dto
      );

      Assertions.assertThat(result.getFunder()).isNull();
    }

    @Test
    void createsObjectWithNullObjectType() {
      var dto = buildValidDto();
      dto.setObjectType(null);

      DigitalObject result = digitalObjectService.create(
          testDataSet.project().getProjectAbbr(), dto
      );

      Assertions.assertThat(result.getObjectType()).isNull();
    }

    // --- ID composition ---

    @Test
    void composesIdFromProjectAbbrAndIdSuffix() {
      var dto = buildValidDto();
      dto.setIdSuffix("my.complex-suffix-123");

      DigitalObject result = digitalObjectService.create(
          testDataSet.project().getProjectAbbr(), dto
      );

      Assertions.assertThat(result.getId())
          .isEqualTo(testDataSet.project().getProjectAbbr() + ".my.complex-suffix-123");
    }
  }

  @Nested
  class UpdateDigitalObject {

    @Test
    void updatesTitle() {
      var patch = new DigitalObjectUpdateDto();
      patch.setTitle("New Title");

      var result = digitalObjectService.updateDigitalObject(
          testDataSet.digitalObject().getId(), patch
      );

      Assertions.assertThat(result.getBaseMetadata().getTitle()).isEqualTo("New Title");

      // verify via repository
      DigitalObject persisted = digitalObjectRepository.findById(
          testDataSet.digitalObject().getId()
      ).orElseThrow();
      Assertions.assertThat(persisted.getBaseMetadata().getTitle()).isEqualTo("New Title");
    }

    @Test
    void updatesMultipleFieldsSimultaneously() {
      var patch = new DigitalObjectUpdateDto();
      patch.setTitle("Updated Title");
      patch.setDescription("Updated Description");
      patch.setRights("Updated Rights");
      patch.setFunder("Updated Funder");
      patch.setObjectType("Updated Type");

      var result = digitalObjectService.updateDigitalObject(
          testDataSet.digitalObject().getId(), patch
      );

      Assertions.assertThat(result.getBaseMetadata().getTitle()).isEqualTo("Updated Title");
      Assertions.assertThat(result.getBaseMetadata().getDescription()).isEqualTo("Updated Description");
      Assertions.assertThat(result.getBaseMetadata().getRights()).isEqualTo("Updated Rights");
      Assertions.assertThat(result.getFunder()).isEqualTo("Updated Funder");
      Assertions.assertThat(result.getObjectType()).isEqualTo("Updated Type");
    }

    @Test
    void preservesUnchangedFields() {
      String originalRights = testDataSet.digitalObject().getBaseMetadata().getRights();
      String originalCreator = testDataSet.digitalObject().getBaseMetadata().getCreator();
      String originalPublisher = testDataSet.digitalObject().getPublisher();
      String originalFunder = testDataSet.digitalObject().getFunder();

      var patch = new DigitalObjectUpdateDto();
      patch.setTitle("Only title changes");

      digitalObjectService.updateDigitalObject(
          testDataSet.digitalObject().getId(), patch
      );

      DigitalObject persisted = digitalObjectRepository.findById(
          testDataSet.digitalObject().getId()
      ).orElseThrow();
      Assertions.assertThat(persisted.getBaseMetadata().getTitle()).isEqualTo("Only title changes");
      Assertions.assertThat(persisted.getBaseMetadata().getRights()).isEqualTo(originalRights);
      Assertions.assertThat(persisted.getBaseMetadata().getCreator()).isEqualTo(originalCreator);
      Assertions.assertThat(persisted.getPublisher()).isEqualTo(originalPublisher);
      Assertions.assertThat(persisted.getFunder()).isEqualTo(originalFunder);
    }

    @Test
    void updatesTags() {
      Set<String> newTags = Set.of("new-tag1", "new-tag2");

      var patch = new DigitalObjectUpdateDto();
      patch.setTags(newTags);

      digitalObjectService.updateDigitalObject(
          testDataSet.digitalObject().getId(), patch
      );

      DigitalObject persisted = digitalObjectRepository.findById(
          testDataSet.digitalObject().getId()
      ).orElseThrow();
      Assertions.assertThat(persisted.getTags())
          .containsExactlyInAnyOrder("new-tag1", "new-tag2");
    }

    @Test
    void removesAllTags() {
      // precondition
      Assertions.assertThat(testDataSet.digitalObject().getTags()).isNotEmpty();

      var patch = new DigitalObjectUpdateDto();
      patch.setTags(new HashSet<>());

      digitalObjectService.updateDigitalObject(
          testDataSet.digitalObject().getId(), patch
      );

      DigitalObject persisted = digitalObjectRepository.findById(
          testDataSet.digitalObject().getId()
      ).orElseThrow();
      Assertions.assertThat(persisted.getTags()).isEmpty();
    }

    @Test
    void tagsUnchangedWhenNotInPatch() {
      Set<String> originalTags = Set.copyOf(testDataSet.digitalObject().getTags());

      var patch = new DigitalObjectUpdateDto();
      patch.setTitle("Tags should survive");

      digitalObjectService.updateDigitalObject(
          testDataSet.digitalObject().getId(), patch
      );

      DigitalObject persisted = digitalObjectRepository.findById(
          testDataSet.digitalObject().getId()
      ).orElseThrow();
      Assertions.assertThat(persisted.getTags())
          .containsExactlyInAnyOrderElementsOf(originalTags);
    }

    @Test
    void throwsNotFoundForNonExistentObject() {
      var patch = new DigitalObjectUpdateDto();
      patch.setTitle("irrelevant");

      Assertions.assertThatThrownBy(
          () -> digitalObjectService.updateDigitalObject("nonexistent.id", patch)
      ).isInstanceOf(DigitalObjectNotFoundException.class);
    }

    @Test
    void rejectsEmptyTitle() {
      var patch = new DigitalObjectUpdateDto();
      patch.setTitle("");

      Assertions.assertThatThrownBy(
              () -> digitalObjectService.updateDigitalObject(
                  testDataSet.digitalObject().getId(), patch
              )
          ).isInstanceOf(DigitalObjectValidationException.class)
          .hasMessageContaining("Title");
    }

    @Test
    void rejectsEmptyRights() {
      var patch = new DigitalObjectUpdateDto();
      patch.setRights("");

      Assertions.assertThatThrownBy(
              () -> digitalObjectService.updateDigitalObject(
                  testDataSet.digitalObject().getId(), patch
              )
          ).isInstanceOf(DigitalObjectValidationException.class)
          .hasMessageContaining("Rights");
    }

    @Test
    void rejectsEmptyCreator() {
      var patch = new DigitalObjectUpdateDto();
      patch.setCreator("");

      Assertions.assertThatThrownBy(
              () -> digitalObjectService.updateDigitalObject(
                  testDataSet.digitalObject().getId(), patch
              )
          ).isInstanceOf(DigitalObjectValidationException.class)
          .hasMessageContaining("Creator");
    }

    @Test
    void rejectsEmptyPublisher() {
      var patch = new DigitalObjectUpdateDto();
      patch.setPublisher("");

      Assertions.assertThatThrownBy(
              () -> digitalObjectService.updateDigitalObject(
                  testDataSet.digitalObject().getId(), patch
              )
          ).isInstanceOf(DigitalObjectValidationException.class)
          .hasMessageContaining("Publisher");
    }

    @Test
    void reportsMultipleViolationsAtOnce() {
      var patch = new DigitalObjectUpdateDto();
      patch.setTitle("");
      patch.setRights("");
      patch.setCreator("");

      Assertions.assertThatThrownBy(
              () -> digitalObjectService.updateDigitalObject(
                  testDataSet.digitalObject().getId(), patch
              )
          ).isInstanceOf(DigitalObjectValidationException.class)
          .hasMessageContaining("Title")
          .hasMessageContaining("Rights")
          .hasMessageContaining("Creator");
    }

    @Test
    void updatesModificationTimestamp() throws InterruptedException {
      Instant beforeUpdate = Instant.now();
      Thread.sleep(50);

      digitalObjectService.updateDigitalObject(
          testDataSet.digitalObject().getId(),
          new DigitalObjectUpdateDto() {{
            setTitle("Timestamp test");
          }}
      );

      DigitalObject persisted = digitalObjectRepository.findById(
          testDataSet.digitalObject().getId()
      ).orElseThrow();
      Assertions.assertThat(persisted.getModified()).isAfter(beforeUpdate);
    }

    @Test
    void returnsCompactDTOWithUpdatedValues() {
      var patch = new DigitalObjectUpdateDto();
      patch.setTitle("DTO check title");
      patch.setFunder("DTO check funder");

      var result = digitalObjectService.updateDigitalObject(
          testDataSet.digitalObject().getId(), patch
      );

      Assertions.assertThat(result).isNotNull();
      Assertions.assertThat(result.getId()).isEqualTo(testDataSet.digitalObject().getId());
      Assertions.assertThat(result.getBaseMetadata().getTitle()).isEqualTo("DTO check title");
      Assertions.assertThat(result.getFunder()).isEqualTo("DTO check funder");
    }

    @Test
    void allowsEmptyDescription() {
      // description is optional — setting it to empty should not throw
      var patch = new DigitalObjectUpdateDto();
      patch.setDescription("");

      var result = digitalObjectService.updateDigitalObject(
          testDataSet.digitalObject().getId(), patch
      );

      Assertions.assertThat(result.getBaseMetadata().getDescription()).isEmpty();
    }

    @Test
    void allowsNullDescription() {
      // null description in patch means "don't change" — original should be preserved
      String originalDescription = testDataSet.digitalObject().getBaseMetadata().getDescription();

      var patch = new DigitalObjectUpdateDto();
      patch.setTitle("Desc null test");
      // description intentionally not set (stays null)

      digitalObjectService.updateDigitalObject(
          testDataSet.digitalObject().getId(), patch
      );

      DigitalObject persisted = digitalObjectRepository.findById(
          testDataSet.digitalObject().getId()
      ).orElseThrow();
      Assertions.assertThat(persisted.getBaseMetadata().getDescription())
          .isEqualTo(originalDescription);
    }


    @Nested
    class UpdateMainResource {

      @Test
      void setsMainResourceToExistingDatastream() {
        var patch = new DigitalObjectUpdateDto();
        patch.setMainResource(testDataSet.mainDatastream().getDsid());

        var result = digitalObjectService.updateDigitalObject(
            testDataSet.digitalObject().getId(), patch
        );

        Assertions.assertThat(result.getMainResource()).isNotNull();
        Assertions.assertThat(result.getMainResource().getDsid())
            .isEqualTo(testDataSet.mainDatastream().getDsid());

        // Verify persistence
        DigitalObject persisted = digitalObjectRepository.findById(
            testDataSet.digitalObject().getId()
        ).orElseThrow();
        Assertions.assertThat(persisted.getMainResource())
            .isEqualTo(testDataSet.mainDatastream().getDsid());
      }

      @Test
      void rejectsNonExistentDatastreamAsDsid() {
        var patch = new DigitalObjectUpdateDto();
        patch.setMainResource("DOES_NOT_EXIST");

        Assertions.assertThatThrownBy(
                () -> digitalObjectService.updateDigitalObject(
                    testDataSet.digitalObject().getId(), patch
                )
            ).isInstanceOf(DigitalObjectValidationException.class)
            .hasMessageContaining("DOES_NOT_EXIST")
            .hasMessageContaining("does not exist");
      }

      @Test
      void clearsMainResourceWithEmptyString() {
        // First set a main resource
        DigitalObject obj = digitalObjectRepository.findById(
            testDataSet.digitalObject().getId()
        ).orElseThrow();
        obj.setMainResource(testDataSet.mainDatastream().getDsid());
        digitalObjectRepository.save(obj);

        // Then clear it
        var patch = new DigitalObjectUpdateDto();
        patch.setMainResource("");

        digitalObjectService.updateDigitalObject(
            testDataSet.digitalObject().getId(), patch
        );

        DigitalObject persisted = digitalObjectRepository.findById(
            testDataSet.digitalObject().getId()
        ).orElseThrow();
        Assertions.assertThat(persisted.getMainResource()).isNull();
      }

      @Test
      void preservesMainResourceWhenNotInPatch() {
        // Set a main resource first
        DigitalObject obj = digitalObjectRepository.findById(
            testDataSet.digitalObject().getId()
        ).orElseThrow();
        obj.setMainResource(testDataSet.mainDatastream().getDsid());
        digitalObjectRepository.save(obj);

        // Patch something else
        var patch = new DigitalObjectUpdateDto();
        patch.setTitle("Unrelated change");

        digitalObjectService.updateDigitalObject(
            testDataSet.digitalObject().getId(), patch
        );

        DigitalObject persisted = digitalObjectRepository.findById(
            testDataSet.digitalObject().getId()
        ).orElseThrow();
        Assertions.assertThat(persisted.getMainResource())
            .isEqualTo(testDataSet.mainDatastream().getDsid());
      }
    }
  }

}
