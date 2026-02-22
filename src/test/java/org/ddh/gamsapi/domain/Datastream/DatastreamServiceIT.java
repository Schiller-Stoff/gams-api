package org.ddh.gamsapi.domain.Datastream;

import org.ddh.gamsapi.IntegrationTest;
import org.ddh.gamsapi.TestUtilities.*;
import org.ddh.gamsapi.domain.Datastream.utils.dto.DatastreamCreateDto;
import org.ddh.gamsapi.domain.Datastream.utils.dto.DatastreamUpdateDto;
import org.ddh.gamsapi.domain.Datastream.utils.exceptions.DatastreamAlreadyExistsException;
import org.ddh.gamsapi.domain.Datastream.utils.exceptions.DatastreamNotFoundException;
import org.ddh.gamsapi.domain.Datastream.utils.exceptions.DatastreamValidationException;
import org.ddh.gamsapi.domain.Datastream.utils.interfaces.IDatastreamContentRepository;
import org.ddh.gamsapi.domain.Datastream.utils.interfaces.IDatastreamRepository;
import org.ddh.gamsapi.domain.Datastream.utils.interfaces.IDatastreamService;
import org.ddh.gamsapi.domain.DigitalObject.DigitalObject;
import org.ddh.gamsapi.domain.DigitalObject.utils.exceptions.DigitalObjectNotFoundException;
import org.ddh.gamsapi.domain.DigitalObject.utils.interfaces.IDigitalObjectRepository;
import org.ddh.gamsapi.domain.Project.interfaces.IProjectRepository;
import org.ddh.gamsapi.infrastructure.System.security.IUserPrincipalAuditorMapping;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.auditing.AuditingHandler;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Date;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class DatastreamServiceIT extends IntegrationTest {

  @Autowired
  IDatastreamService datastreamService;

  @Autowired
  IDatastreamRepository datastreamRepository;

  @Autowired
  IDatastreamContentRepository datastreamContentRepository;

  @Autowired
  IDigitalObjectRepository  digitalObjectRepository;

  @Autowired
  IProjectRepository projectRepository;

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

  final private MockMultipartFile TEST_MULTIPART_FILE = TestDatastreamContent.generate();

  @BeforeEach
  public void setup(){
    testDataSet = testDataBuilder.buildTestDataSet();
    // needed when changing datastreams
    Mockito.when(userPrincipalAuditorMapping.getCurrentAuditor())
        .thenReturn(Optional.of(TestUser.USERNAME.getValue()));
  }

  @Nested
  public class SaveDatastream {

    @Test
    public void throwsIfReferencedDigitalObjectNotFound(){

      final DigitalObject digitalObject = TestDigitalObject.generate();
      digitalObject.setId("SOME_RANDOM_PID");

      Datastream datastream = TestDatastream.generate(digitalObject);

      Assertions.assertThrows(
          DigitalObjectNotFoundException.class,
          () -> datastreamService.save(datastream, TEST_MULTIPART_FILE)
      );

    }

    @Test
    public void datastreamExistsAfterSaving(){

      final String RANDOM_DSID = "SOME_RANDOM_DSID.txt";
      Datastream datastream = TestDatastream.generate(testDataSet.digitalObject(), RANDOM_DSID);

      org.assertj.core.api.Assertions.assertThat(
          datastreamRepository.findById(datastream.deriveDatastreamId())
      ).isNotNull().isEmpty();

      datastreamService.save(datastream, TEST_MULTIPART_FILE);

      org.assertj.core.api.Assertions.assertThat(
          datastreamRepository.findById(datastream.deriveDatastreamId())
      ).isNotNull().isPresent();

    }

    @Test
    public void savedDatastreamHasExpectedTagProperty(){
      final String RANDOM_DSID = "SOME_RANDOM_DSID.txt";
      Datastream datastream = TestDatastream.generate(testDataSet.digitalObject(), RANDOM_DSID);

      org.assertj.core.api.Assertions.assertThat(
          datastreamRepository.findById(datastream.deriveDatastreamId())
      ).isNotNull().isEmpty();

      Datastream savedDatastream = datastreamService.save(datastream, TEST_MULTIPART_FILE);
      org.assertj.core.api.Assertions.assertThat(savedDatastream)
          .isNotNull()
          .extracting(Datastream::getTags)
          .isEqualTo(datastream.getTags());

    }

    @Test
    public void savedDatastreamHasExpectedLangProperty(){
      final String RANDOM_DSID = "SOME_RANDOM_DSID.txt";
      Datastream datastream = TestDatastream.generate(testDataSet.digitalObject(), RANDOM_DSID);

      org.assertj.core.api.Assertions.assertThat(
          datastreamRepository.findById(datastream.deriveDatastreamId())
      ).isNotNull().isEmpty();

      Datastream savedDatastream = datastreamService.save(datastream, TEST_MULTIPART_FILE);
      org.assertj.core.api.Assertions.assertThat(savedDatastream)
          .isNotNull()
          .extracting(Datastream::getLang)
          .isEqualTo(datastream.getLang());

    }

    @Test
    public void saveDatastreamChangesModifiedDateOfParentObject(){

      // first assert that expected property is false by default
      org.assertj.core.api.Assertions.assertThat(testDataSet.digitalObject().isModifiedAfterCreation())
          .isFalse();

      // capture the original modified date of the parent digital object
      Date originalModified = testDataSet.digitalObject().getModified();

      // small delay to ensure timestamp difference
      try { Thread.sleep(50); } catch (InterruptedException ignored) {}

      // save a new datastream
      final String RANDOM_DSID = "MODIFICATION_TEST.txt";
      Datastream datastream = TestDatastream.generate(testDataSet.digitalObject(), RANDOM_DSID);
      datastreamService.save(datastream, TEST_MULTIPART_FILE);

      // re-fetch the parent from DB to get the updated modified date
      DigitalObject refreshedParent = digitalObjectRepository
          .findById(testDataSet.digitalObject().getId())
          .orElseThrow();

      // modified date should be after created
      org.assertj.core.api.Assertions.assertThat(refreshedParent.getModified())
          .isAfter(originalModified);

      // createdAfterModification property should be true now
      org.assertj.core.api.Assertions.assertThat(refreshedParent.isModifiedAfterCreation())
          .isTrue();

    }

    @Test
    public void saveDatastreamChangesModifiedOfParentProject(){

      // capture the original modified date of the parent digital object
      Date originalModified = testDataSet.project().getModified();

      // small delay to ensure timestamp difference
      try { Thread.sleep(50); } catch (InterruptedException ignored) {}

      // save a new datastream
      final String RANDOM_DSID = "MODIFICATION_TEST.txt";
      Datastream datastream = TestDatastream.generate(testDataSet.digitalObject(), RANDOM_DSID);
      datastreamService.save(datastream, TEST_MULTIPART_FILE);

      // re-fetch the parent project from DB to get the updated modified date
      var refreshedParentProject = projectRepository
          .findById(testDataSet.project().getProjectAbbr())
          .orElseThrow();

      // modified date should be after created
      org.assertj.core.api.Assertions.assertThat(refreshedParentProject.getModified())
          .isAfter(originalModified);

    }

  }

  @Nested
  public class DeleteDatastream {
    @Test
    public void successfullyDeletesDatastream() {

      // actual deletion
      datastreamService.delete(testDataSet.mainDatastream());

      // check if datastream is deleted
      org.assertj.core.api.Assertions.assertThat(datastreamRepository.findById(testDataSet.mainDatastream().deriveDatastreamId()))
          .isNotNull()
          .isEmpty();

      // check if datastream content is also deleted
      org.assertj.core.api.Assertions.assertThat(datastreamContentRepository.exists(testDataSet.mainDatastream().deriveDatastreamId()))
          .isFalse();

    }

    @Test
    public void deleteThrowsWhenDigitalObjectIsNull() {
      Datastream datastream = new DatastreamBuilder()
          .dsid("SOME_RANDOM_DSID")
          .baseMetadata(TestMetadataBaseEntity.generate())
          .build();

      Assertions.assertThrows(
          DigitalObjectNotFoundException.class,
          () -> datastreamService.delete(datastream)
      );
    }

    @Test
    public void deleteThrowsWhenDatastreamDoesNotExist() {
      Datastream datastream = new DatastreamBuilder()
          .dsid("SOME_RANDOM_DSID")
          .digitalObject(testDataSet.digitalObject())
          .baseMetadata(TestMetadataBaseEntity.generate())
          .build();

      Assertions.assertThrows(
          DatastreamNotFoundException.class,
          () -> datastreamService.delete(datastream)
      );
    }

    @Test
    public void deleteDatastreamShouldChangeModifiedOfDigitalObject(){
      // first assert that expected property is false by default
      org.assertj.core.api.Assertions.assertThat(testDataSet.digitalObject().isModifiedAfterCreation())
          .isFalse();

      // capture the original modified date of the parent digital object
      Date originalModified = testDataSet.digitalObject().getModified();

      // small delay to ensure timestamp difference
      try { Thread.sleep(50); } catch (InterruptedException ignored) {}

      // delete test datastream
      datastreamService.delete(testDataSet.mainDatastream());

      // re-fetch the parent from DB to get the updated modified date
      DigitalObject refreshedParent = digitalObjectRepository
          .findById(testDataSet.digitalObject().getId())
          .orElseThrow();

      // modified date should be after created
      org.assertj.core.api.Assertions.assertThat(refreshedParent.getModified())
          .isAfter(originalModified);

      // createdAfterModification property should be true now
      org.assertj.core.api.Assertions.assertThat(refreshedParent.isModifiedAfterCreation())
          .isTrue();
    }

    @Test
    public void deleteDatastreamShouldChangeParentProjectModified(){

      // capture the original modified date of the parent digital object
      Date originalModified = testDataSet.project().getModified();

      // small delay to ensure timestamp difference
      try { Thread.sleep(50); } catch (InterruptedException ignored) {}

      // delete test datastream
      datastreamService.delete(testDataSet.mainDatastream());

      var refreshedProject = projectRepository.findById(testDataSet.project().getProjectAbbr())
          .orElseThrow();

      org.assertj.core.api.Assertions.assertThat(refreshedProject.getModified())
          .isAfter(originalModified);


    }

    @Test
    public void deleteDatastreamChangesModifiedByOfParentProject(){
      // capture the original modified date of the parent digital object
      String originalModifiedBy = testDataSet.project().getModifiedBy();

      org.assertj.core.api.Assertions.assertThat(originalModifiedBy)
          .isNotEqualTo(TestUser.USERNAME.getValue());

      // delete test datastream
      datastreamService.delete(testDataSet.mainDatastream());

      var refreshedProject = projectRepository.findById(testDataSet.project().getProjectAbbr())
          .orElseThrow();

      org.assertj.core.api.Assertions.assertThat(refreshedProject.getModifiedBy())
          .isEqualTo(TestUser.USERNAME.getValue());
    }

  }

  @Nested
  public class FindAll {

    @Test
    public void returnsExpectedCountOfDatastreams(){
      testDataBuilder.addRandomDatastream(testDataSet);
      org.assertj.core.api.Assertions.assertThat(datastreamService.findAll(testDataSet.digitalObject()))
          .isNotNull()
          .isNotEmpty()
          .hasSize(2);
    }

  }

  @Nested
  public class FindById {

    @Test
    public void returnsExpectedDatastream(){
      org.assertj.core.api.Assertions.assertThat(
          datastreamService.findById(testDataSet.mainDatastream().deriveDatastreamId()))
            .isNotNull()
            .isEqualTo(testDataSet.mainDatastream());
    }

    @Test
    public void throwsIfDatastreamNotFound(){

      DatastreamId randomId = new DatastreamId(
          "SOME_RANDOM_PID",
          "SOME_RANDOM_DSID");

      Assertions.assertThrows(
          DatastreamNotFoundException.class,
          () -> datastreamService.findById(randomId)
      );

    }


  }

  @Nested
  public class FindDatastreamDetailsById {

    @Test
    public void returnsExpectedDatastreamDetailsView(){

      org.assertj.core.api.Assertions.assertThat(
          datastreamService.findDatastreamDetailsById(testDataSet.mainDatastream().deriveDatastreamId()))
          .isNotNull()
          .extracting("dsid")
          .isEqualTo(testDataSet.mainDatastream().getDsid());

    }

    @Test
    public void throwsIfDatastreamDetailsViewNotFound(){

      DatastreamId randomId = new DatastreamId("SOME_RANDOM_PID", "SOME_RANDOM_DSID");

      Assertions.assertThrows(
          DatastreamNotFoundException.class,
          () -> datastreamService.findDatastreamDetailsById(randomId)
      );

    }

  }


  @Nested
  public class CreateFromUpload {

    @Test
    public void createsDatastreamWithServerComputedChecksums() {
      DatastreamCreateDto dto = new DatastreamCreateDto();
      dto.setTitle("Test Image");
      dto.setCreator("Test Creator");
      dto.setRights("CC BY 4.0");
      dto.setDescription("Test description");

      Datastream created = datastreamService.createFromUpload(
          testDataSet.digitalObject().getId(),
          "uploaded_file.txt",
          dto,
          TEST_MULTIPART_FILE
      );

      org.assertj.core.api.Assertions.assertThat(created).isNotNull();
      org.assertj.core.api.Assertions.assertThat(created.getDsid()).isEqualTo("uploaded_file.txt");
      org.assertj.core.api.Assertions.assertThat(created.getMd5Checksum()).isNotEmpty();
      org.assertj.core.api.Assertions.assertThat(created.getSha512Checksum()).isNotEmpty();
      org.assertj.core.api.Assertions.assertThat(created.getSize()).isEqualTo(TEST_MULTIPART_FILE.getSize());
    }

    @Test
    public void setsMetadataFromDto() {
      DatastreamCreateDto dto = new DatastreamCreateDto();
      dto.setTitle("My Title");
      dto.setCreator("My Creator");
      dto.setRights("CC BY 4.0");
      dto.setDescription("My Description");

      Datastream created = datastreamService.createFromUpload(
          testDataSet.digitalObject().getId(),
          "metadata_test.txt",
          dto,
          TEST_MULTIPART_FILE
      );

      org.assertj.core.api.Assertions.assertThat(created.getBaseMetadata().getTitle()).isEqualTo("My Title");
      org.assertj.core.api.Assertions.assertThat(created.getBaseMetadata().getCreator()).isEqualTo("My Creator");
      org.assertj.core.api.Assertions.assertThat(created.getBaseMetadata().getRights()).isEqualTo("CC BY 4.0");
      org.assertj.core.api.Assertions.assertThat(created.getBaseMetadata().getDescription()).isEqualTo("My Description");
    }

    @Test
    public void setsSyntheticBagPath() {
      DatastreamCreateDto dto = new DatastreamCreateDto();
      dto.setTitle("Test");
      dto.setCreator("Creator");
      dto.setRights("Rights");

      Datastream created = datastreamService.createFromUpload(
          testDataSet.digitalObject().getId(),
          "photo.jpg",
          dto,
          new MockMultipartFile("file", "photo.jpg", "image/jpeg", "data".getBytes())
      );

      org.assertj.core.api.Assertions.assertThat(created.getFilePath()).isEqualTo("photo.jpg");
    }

    @Test
    public void throwsIfDigitalObjectNotFound() {
      DatastreamCreateDto dto = new DatastreamCreateDto();
      dto.setTitle("Test");
      dto.setCreator("Creator");
      dto.setRights("Rights");

      org.assertj.core.api.Assertions.assertThatThrownBy(
          () -> datastreamService.createFromUpload(
              "nonexistent.id", "test.txt", dto, TEST_MULTIPART_FILE
          )
      ).isInstanceOf(DigitalObjectNotFoundException.class);
    }

    @Test
    public void throwsIfDuplicateDsid() {
      DatastreamCreateDto dto = new DatastreamCreateDto();
      dto.setTitle("Test");
      dto.setCreator("Creator");
      dto.setRights("Rights");

      // Use the same dsid as the existing main datastream
      String existingDsid = testDataSet.mainDatastream().getDsid();

      org.assertj.core.api.Assertions.assertThatThrownBy(
          () -> datastreamService.createFromUpload(
              testDataSet.digitalObject().getId(),
              existingDsid,
              dto,
              TEST_MULTIPART_FILE
          )
      ).isInstanceOf(DatastreamAlreadyExistsException.class);
    }

    @Test
    public void throwsIfFileIsEmpty() {
      DatastreamCreateDto dto = new DatastreamCreateDto();
      dto.setTitle("Test");
      dto.setCreator("Creator");
      dto.setRights("Rights");

      MockMultipartFile emptyFile = new MockMultipartFile(
          "file", "empty.txt", "text/plain", new byte[0]
      );

      org.assertj.core.api.Assertions.assertThatThrownBy(
          () -> datastreamService.createFromUpload(
              testDataSet.digitalObject().getId(),
              "empty.txt",
              dto,
              emptyFile
          )
      ).isInstanceOf(DatastreamValidationException.class);
    }

    @Test
    public void throwsIfDsidIsBlank() {
      DatastreamCreateDto dto = new DatastreamCreateDto();
      dto.setTitle("Test");
      dto.setCreator("Creator");
      dto.setRights("Rights");

      org.assertj.core.api.Assertions.assertThatThrownBy(
          () -> datastreamService.createFromUpload(
              testDataSet.digitalObject().getId(),
              "   ",
              dto,
              TEST_MULTIPART_FILE
          )
      ).isInstanceOf(DatastreamValidationException.class);
    }

    @Test
    public void defaultsTagsAndLangToEmptySets() {
      DatastreamCreateDto dto = new DatastreamCreateDto();
      dto.setTitle("Test");
      dto.setCreator("Creator");
      dto.setRights("Rights");

      Datastream created = datastreamService.createFromUpload(
          testDataSet.digitalObject().getId(),
          "empty_collections.txt",
          dto,
          TEST_MULTIPART_FILE
      );

      org.assertj.core.api.Assertions.assertThat(created.getTags()).isNotNull().isEmpty();
      org.assertj.core.api.Assertions.assertThat(created.getLang()).isNotNull().isEmpty();
    }

    @Test
    public void updatesParentObjectModifiedTimestamp() throws InterruptedException {
      Date originalModified = testDataSet.digitalObject().getModified();
      Thread.sleep(50);

      DatastreamCreateDto dto = new DatastreamCreateDto();
      dto.setTitle("Test");
      dto.setCreator("Creator");
      dto.setRights("Rights");

      datastreamService.createFromUpload(
          testDataSet.digitalObject().getId(),
          "timestamp_test.txt",
          dto,
          TEST_MULTIPART_FILE
      );

      DigitalObject refreshed = digitalObjectRepository
          .findById(testDataSet.digitalObject().getId()).orElseThrow();

      org.assertj.core.api.Assertions.assertThat(refreshed.getModified()).isAfter(originalModified);
    }

    @Test
    public void updatesParentProjectModifiedTimestamp() throws InterruptedException {
      Date originalModified = testDataSet.project().getModified();
      Thread.sleep(50);

      DatastreamCreateDto dto = new DatastreamCreateDto();
      dto.setTitle("Test");
      dto.setCreator("Creator");
      dto.setRights("Rights");

      datastreamService.createFromUpload(
          testDataSet.digitalObject().getId(),
          "project_ts_test.txt",
          dto,
          TEST_MULTIPART_FILE
      );

      var refreshedProject = projectRepository
          .findById(testDataSet.project().getProjectAbbr()).orElseThrow();

      org.assertj.core.api.Assertions.assertThat(refreshedProject.getModified()).isAfter(originalModified);
    }

    @Test
    public void fileContentIsPersisted() {
      DatastreamCreateDto dto = new DatastreamCreateDto();
      dto.setTitle("Test");
      dto.setCreator("Creator");
      dto.setRights("Rights");

      final String DSID = "persisted_content.txt";

      datastreamService.createFromUpload(
          testDataSet.digitalObject().getId(),
          DSID,
          dto,
          TEST_MULTIPART_FILE
      );

      DatastreamId dsId = new DatastreamId(DSID, testDataSet.digitalObject().getId());
      org.assertj.core.api.Assertions.assertThat(datastreamContentRepository.exists(dsId)).isTrue();
    }
  }

  // ==================================================================================
  // Add to DatastreamServiceIT.java — as a sibling of the existing @Nested classes
  // ==================================================================================
  // Additional import needed:
  // import org.ddh.gamsapi.domain.Datastream.utils.dto.DatastreamUpdateDto;

  @Nested
  public class UpdateDatastream {

    @Test
    public void updatesTitle() {
      var patch = new DatastreamUpdateDto();
      patch.setTitle("Updated Title");

      var result = datastreamService.updateDatastream(
          testDataSet.digitalObject().getId(),
          testDataSet.mainDatastream().getDsid(),
          patch
      );

      org.assertj.core.api.Assertions.assertThat(result.getBaseMetadata().getTitle())
          .isEqualTo("Updated Title");
    }

    @Test
    public void updatesMultipleMetadataFields() {
      var patch = new DatastreamUpdateDto();
      patch.setTitle("New Title");
      patch.setDescription("New Description");
      patch.setRights("New Rights");
      patch.setCreator("New Creator");

      var result = datastreamService.updateDatastream(
          testDataSet.digitalObject().getId(),
          testDataSet.mainDatastream().getDsid(),
          patch
      );

      org.assertj.core.api.Assertions.assertThat(result.getBaseMetadata().getTitle())
          .isEqualTo("New Title");
      org.assertj.core.api.Assertions.assertThat(result.getBaseMetadata().getDescription())
          .isEqualTo("New Description");
      org.assertj.core.api.Assertions.assertThat(result.getBaseMetadata().getRights())
          .isEqualTo("New Rights");
      org.assertj.core.api.Assertions.assertThat(result.getBaseMetadata().getCreator())
          .isEqualTo("New Creator");
    }

    @Test
    public void preservesUnchangedFields() {
      String originalRights = testDataSet.mainDatastream().getBaseMetadata().getRights();
      String originalCreator = testDataSet.mainDatastream().getBaseMetadata().getCreator();

      var patch = new DatastreamUpdateDto();
      patch.setTitle("Only title changes");

      datastreamService.updateDatastream(
          testDataSet.digitalObject().getId(),
          testDataSet.mainDatastream().getDsid(),
          patch
      );

      Datastream persisted = datastreamRepository.findById(
          testDataSet.mainDatastream().deriveDatastreamId()
      ).orElseThrow();

      org.assertj.core.api.Assertions.assertThat(persisted.getBaseMetadata().getTitle())
          .isEqualTo("Only title changes");
      org.assertj.core.api.Assertions.assertThat(persisted.getBaseMetadata().getRights())
          .isEqualTo(originalRights);
      org.assertj.core.api.Assertions.assertThat(persisted.getBaseMetadata().getCreator())
          .isEqualTo(originalCreator);
    }

    @Test
    public void updatesTags() {
      Set<String> newTags = Set.of("updated-tag1", "updated-tag2");

      var patch = new DatastreamUpdateDto();
      patch.setTags(newTags);

      datastreamService.updateDatastream(
          testDataSet.digitalObject().getId(),
          testDataSet.mainDatastream().getDsid(),
          patch
      );

      Datastream persisted = datastreamRepository.findById(
          testDataSet.mainDatastream().deriveDatastreamId()
      ).orElseThrow();

      org.assertj.core.api.Assertions.assertThat(persisted.getTags())
          .containsExactlyInAnyOrder("updated-tag1", "updated-tag2");
    }

    @Test
    public void removesAllTags() {
      // precondition: tags are not empty
      org.assertj.core.api.Assertions.assertThat(testDataSet.mainDatastream().getTags())
          .isNotEmpty();

      var patch = new DatastreamUpdateDto();
      patch.setTags(new HashSet<>());

      datastreamService.updateDatastream(
          testDataSet.digitalObject().getId(),
          testDataSet.mainDatastream().getDsid(),
          patch
      );

      Datastream persisted = datastreamRepository.findById(
          testDataSet.mainDatastream().deriveDatastreamId()
      ).orElseThrow();

      org.assertj.core.api.Assertions.assertThat(persisted.getTags()).isEmpty();
    }

    @Test
    public void tagsUnchangedWhenNotInPatch() {
      // Re-fetch to safely access lazy tags collection
      Datastream fresh = datastreamRepository.findById(
          testDataSet.mainDatastream().deriveDatastreamId()
      ).orElseThrow();
      Set<String> originalTags = Set.copyOf(fresh.getTags());

      var patch = new DatastreamUpdateDto();
      patch.setTitle("Tags should survive");

      datastreamService.updateDatastream(
          testDataSet.digitalObject().getId(),
          testDataSet.mainDatastream().getDsid(),
          patch
      );

      Datastream persisted = datastreamRepository.findById(
          testDataSet.mainDatastream().deriveDatastreamId()
      ).orElseThrow();

      org.assertj.core.api.Assertions.assertThat(persisted.getTags())
          .containsExactlyInAnyOrderElementsOf(originalTags);
    }

    @Test
    public void updatesLang() {
      Set<String> newLang = Set.of("en", "de");

      var patch = new DatastreamUpdateDto();
      patch.setLang(newLang);

      datastreamService.updateDatastream(
          testDataSet.digitalObject().getId(),
          testDataSet.mainDatastream().getDsid(),
          patch
      );

      Datastream persisted = datastreamRepository.findById(
          testDataSet.mainDatastream().deriveDatastreamId()
      ).orElseThrow();

      org.assertj.core.api.Assertions.assertThat(persisted.getLang())
          .containsExactlyInAnyOrder("en", "de");
    }

    @Test
    public void allowsEmptyDescription() {
      var patch = new DatastreamUpdateDto();
      patch.setDescription("");

      var result = datastreamService.updateDatastream(
          testDataSet.digitalObject().getId(),
          testDataSet.mainDatastream().getDsid(),
          patch
      );

      org.assertj.core.api.Assertions.assertThat(result.getBaseMetadata().getDescription())
          .isEmpty();
    }

    @Test
    public void allowsNullDescription() {
      String originalDescription = testDataSet.mainDatastream().getBaseMetadata().getDescription();

      var patch = new DatastreamUpdateDto();
      patch.setTitle("Desc null test");
      // description intentionally not set (stays null)

      datastreamService.updateDatastream(
          testDataSet.digitalObject().getId(),
          testDataSet.mainDatastream().getDsid(),
          patch
      );

      Datastream persisted = datastreamRepository.findById(
          testDataSet.mainDatastream().deriveDatastreamId()
      ).orElseThrow();

      org.assertj.core.api.Assertions.assertThat(persisted.getBaseMetadata().getDescription())
          .isEqualTo(originalDescription);
    }

    @Test
    public void rejectsEmptyTitle() {
      var patch = new DatastreamUpdateDto();
      patch.setTitle("");

      org.assertj.core.api.Assertions.assertThatThrownBy(
              () -> datastreamService.updateDatastream(
                  testDataSet.digitalObject().getId(),
                  testDataSet.mainDatastream().getDsid(),
                  patch
              )
          ).isInstanceOf(DatastreamValidationException.class)
          .hasMessageContaining("Title");
    }

    @Test
    public void rejectsEmptyRights() {
      var patch = new DatastreamUpdateDto();
      patch.setRights("");

      org.assertj.core.api.Assertions.assertThatThrownBy(
              () -> datastreamService.updateDatastream(
                  testDataSet.digitalObject().getId(),
                  testDataSet.mainDatastream().getDsid(),
                  patch
              )
          ).isInstanceOf(DatastreamValidationException.class)
          .hasMessageContaining("Rights");
    }

    @Test
    public void rejectsEmptyCreator() {
      var patch = new DatastreamUpdateDto();
      patch.setCreator("");

      org.assertj.core.api.Assertions.assertThatThrownBy(
              () -> datastreamService.updateDatastream(
                  testDataSet.digitalObject().getId(),
                  testDataSet.mainDatastream().getDsid(),
                  patch
              )
          ).isInstanceOf(DatastreamValidationException.class)
          .hasMessageContaining("Creator");
    }

    @Test
    public void reportsMultipleViolationsAtOnce() {
      var patch = new DatastreamUpdateDto();
      patch.setTitle("");
      patch.setRights("");
      patch.setCreator("");

      org.assertj.core.api.Assertions.assertThatThrownBy(
              () -> datastreamService.updateDatastream(
                  testDataSet.digitalObject().getId(),
                  testDataSet.mainDatastream().getDsid(),
                  patch
              )
          ).isInstanceOf(DatastreamValidationException.class)
          .hasMessageContaining("Title")
          .hasMessageContaining("Rights")
          .hasMessageContaining("Creator");
    }

    @Test
    public void throwsNotFoundForNonExistentDatastream() {
      var patch = new DatastreamUpdateDto();
      patch.setTitle("irrelevant");

      org.assertj.core.api.Assertions.assertThatThrownBy(
          () -> datastreamService.updateDatastream(
              testDataSet.digitalObject().getId(),
              "DOES_NOT_EXIST.txt",
              patch
          )
      ).isInstanceOf(DatastreamNotFoundException.class);
    }

    @Test
    public void throwsNotFoundForNonExistentDigitalObject() {
      var patch = new DatastreamUpdateDto();
      patch.setTitle("irrelevant");

      org.assertj.core.api.Assertions.assertThatThrownBy(
          () -> datastreamService.updateDatastream(
              "nonexistent.object.id",
              testDataSet.mainDatastream().getDsid(),
              patch
          )
      ).isInstanceOf(DigitalObjectNotFoundException.class);
    }

    @Test
    public void updatesParentObjectModifiedTimestamp() throws InterruptedException {
      Date originalModified = testDataSet.digitalObject().getModified();
      Thread.sleep(50);

      var patch = new DatastreamUpdateDto();
      patch.setTitle("Timestamp test");

      datastreamService.updateDatastream(
          testDataSet.digitalObject().getId(),
          testDataSet.mainDatastream().getDsid(),
          patch
      );

      DigitalObject refreshed = digitalObjectRepository
          .findById(testDataSet.digitalObject().getId()).orElseThrow();

      org.assertj.core.api.Assertions.assertThat(refreshed.getModified())
          .isAfter(originalModified);
    }

    @Test
    public void returnsDetailsViewWithUpdatedValues() {
      var patch = new DatastreamUpdateDto();
      patch.setTitle("DTO check title");
      patch.setDescription("DTO check description");

      var result = datastreamService.updateDatastream(
          testDataSet.digitalObject().getId(),
          testDataSet.mainDatastream().getDsid(),
          patch
      );

      org.assertj.core.api.Assertions.assertThat(result).isNotNull();
      org.assertj.core.api.Assertions.assertThat(result.getDsid())
          .isEqualTo(testDataSet.mainDatastream().getDsid());
      org.assertj.core.api.Assertions.assertThat(result.getBaseMetadata().getTitle())
          .isEqualTo("DTO check title");
      org.assertj.core.api.Assertions.assertThat(result.getBaseMetadata().getDescription())
          .isEqualTo("DTO check description");
    }

    @Test
    public void doesNotChangeDsid() {
      String originalDsid = testDataSet.mainDatastream().getDsid();

      var patch = new DatastreamUpdateDto();
      patch.setTitle("dsid should not change");

      datastreamService.updateDatastream(
          testDataSet.digitalObject().getId(),
          originalDsid,
          patch
      );

      // Verify the datastream is still accessible via original dsid
      org.assertj.core.api.Assertions.assertThat(
          datastreamRepository.findById(testDataSet.mainDatastream().deriveDatastreamId())
      ).isPresent();
    }

    @Test
    public void doesNotChangeChecksums() {
      String originalMd5 = testDataSet.mainDatastream().getMd5Checksum();
      String originalSha512 = testDataSet.mainDatastream().getSha512Checksum();

      var patch = new DatastreamUpdateDto();
      patch.setTitle("checksums should not change");

      datastreamService.updateDatastream(
          testDataSet.digitalObject().getId(),
          testDataSet.mainDatastream().getDsid(),
          patch
      );

      Datastream persisted = datastreamRepository.findById(
          testDataSet.mainDatastream().deriveDatastreamId()
      ).orElseThrow();

      org.assertj.core.api.Assertions.assertThat(persisted.getMd5Checksum())
          .isEqualTo(originalMd5);
      org.assertj.core.api.Assertions.assertThat(persisted.getSha512Checksum())
          .isEqualTo(originalSha512);
    }

    @Test
    public void doesNotChangeSize() {
      Long originalSize = testDataSet.mainDatastream().getSize();

      var patch = new DatastreamUpdateDto();
      patch.setTitle("size should not change");

      datastreamService.updateDatastream(
          testDataSet.digitalObject().getId(),
          testDataSet.mainDatastream().getDsid(),
          patch
      );

      Datastream persisted = datastreamRepository.findById(
          testDataSet.mainDatastream().deriveDatastreamId()
      ).orElseThrow();

      org.assertj.core.api.Assertions.assertThat(persisted.getSize())
          .isEqualTo(originalSize);
    }
  }


  @Nested
  public class UpdateDatastreamContent {

    @Test
    public void updatesContentAndRecomputesChecksums() {
      String originalMd5 = testDataSet.mainDatastream().getMd5Checksum();

      MockMultipartFile newFile = new MockMultipartFile(
          "file", "test.txt", "text/plain",
          "completely new content for checksum test".getBytes()
      );

      var result = datastreamService.updateDatastreamContent(
          testDataSet.digitalObject().getId(),
          testDataSet.mainDatastream().getDsid(),
          newFile
      );

      org.assertj.core.api.Assertions.assertThat(result.getMd5Checksum())
          .isNotEqualTo(originalMd5);
      org.assertj.core.api.Assertions.assertThat(result.getMd5Checksum())
          .isNotEmpty();
      org.assertj.core.api.Assertions.assertThat(result.getSha512Checksum())
          .isNotEmpty();
    }

    @Test
    public void updatesFileSize() {
      byte[] newContent = "much longer content than before for size test".getBytes();
      MockMultipartFile newFile = new MockMultipartFile(
          "file", "test.txt", "text/plain", newContent
      );

      var result = datastreamService.updateDatastreamContent(
          testDataSet.digitalObject().getId(),
          testDataSet.mainDatastream().getDsid(),
          newFile
      );

      org.assertj.core.api.Assertions.assertThat(result.getSize())
          .isEqualTo(newContent.length);
    }

    @Test
    public void preservesMetadata() {
      String originalTitle = testDataSet.mainDatastream().getBaseMetadata().getTitle();
      String originalCreator = testDataSet.mainDatastream().getBaseMetadata().getCreator();

      MockMultipartFile newFile = new MockMultipartFile(
          "file", "test.txt", "text/plain", "new content".getBytes()
      );

      datastreamService.updateDatastreamContent(
          testDataSet.digitalObject().getId(),
          testDataSet.mainDatastream().getDsid(),
          newFile
      );

      Datastream persisted = datastreamRepository.findById(
          testDataSet.mainDatastream().deriveDatastreamId()
      ).orElseThrow();

      org.assertj.core.api.Assertions.assertThat(persisted.getBaseMetadata().getTitle())
          .isEqualTo(originalTitle);
      org.assertj.core.api.Assertions.assertThat(persisted.getBaseMetadata().getCreator())
          .isEqualTo(originalCreator);
    }

    @Test
    public void throwsIfFileIsEmpty() {
      MockMultipartFile emptyFile = new MockMultipartFile(
          "file", "empty.txt", "text/plain", new byte[0]
      );

      org.assertj.core.api.Assertions.assertThatThrownBy(
          () -> datastreamService.updateDatastreamContent(
              testDataSet.digitalObject().getId(),
              testDataSet.mainDatastream().getDsid(),
              emptyFile
          )
      ).isInstanceOf(DatastreamValidationException.class);
    }

    @Test
    public void throwsIfDatastreamNotFound() {
      org.assertj.core.api.Assertions.assertThatThrownBy(
          () -> datastreamService.updateDatastreamContent(
              testDataSet.digitalObject().getId(),
              "DOES_NOT_EXIST.txt",
              TEST_MULTIPART_FILE
          )
      ).isInstanceOf(DatastreamNotFoundException.class);
    }

    @Test
    public void throwsIfDigitalObjectNotFound() {
      org.assertj.core.api.Assertions.assertThatThrownBy(
          () -> datastreamService.updateDatastreamContent(
              "nonexistent.object.id",
              testDataSet.mainDatastream().getDsid(),
              TEST_MULTIPART_FILE
          )
      ).isInstanceOf(DigitalObjectNotFoundException.class);
    }

    @Test
    public void updatesParentObjectModifiedTimestamp() throws InterruptedException {
      Date originalModified = testDataSet.digitalObject().getModified();
      Thread.sleep(50);

      datastreamService.updateDatastreamContent(
          testDataSet.digitalObject().getId(),
          testDataSet.mainDatastream().getDsid(),
          TEST_MULTIPART_FILE
      );

      DigitalObject refreshed = digitalObjectRepository
          .findById(testDataSet.digitalObject().getId()).orElseThrow();

      org.assertj.core.api.Assertions.assertThat(refreshed.getModified())
          .isAfter(originalModified);
    }
  }

}
