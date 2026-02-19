package org.ddh.gamsapi.domain.Datastream;

import org.ddh.gamsapi.IntegrationTest;
import org.ddh.gamsapi.TestUtilities.*;
import org.ddh.gamsapi.domain.Datastream.utils.exceptions.DatastreamNotFoundException;
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
import java.util.Optional;

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
        .thenReturn(Optional.of("test-user"));
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


}
