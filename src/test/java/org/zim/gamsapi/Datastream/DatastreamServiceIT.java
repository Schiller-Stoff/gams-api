package org.zim.gamsapi.Datastream;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.auditing.AuditingHandler;
import org.springframework.mock.web.MockMultipartFile;
import org.zim.gamsapi.Datastream.exceptions.DatastreamNotFoundException;
import org.zim.gamsapi.Datastream.interfaces.IDatastreamContentRepository;
import org.zim.gamsapi.Datastream.interfaces.IDatastreamService;
import org.zim.gamsapi.DigitalObject.DigitalObject;
import org.zim.gamsapi.DigitalObject.IDigitalObjectRepository;
import org.zim.gamsapi.DigitalObject.exceptions.DigitalObjectNotFoundException;
import org.zim.gamsapi.IntegrationTest;
import org.zim.gamsapi.Project.Project;
import org.zim.gamsapi.Project.interfaces.IProjectRepository;
import org.zim.gamsapi.enums.TestDatastream;
import org.zim.gamsapi.enums.TestDatastreamContent;
import org.zim.gamsapi.enums.TestDigitalObject;
import org.zim.gamsapi.enums.TestMetadataBaseEntity;
import java.io.IOException;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class DatastreamServiceIT extends IntegrationTest {

  @Autowired
  IDatastreamService datastreamService;
  @Autowired
  IDigitalObjectRepository digitalObjectRepository;

  @Autowired
  IProjectRepository projectRepository;

  @Autowired
  IDatastreamRepository datastreamRepository;

  @Autowired
  IDatastreamContentRepository datastreamContentRepository;

  @MockBean
  private AuditingHandler auditingHandler;

  private DigitalObject testObject;

  private Project testProject;

  final private MockMultipartFile TEST_MULTIPART_FILE = TestDatastreamContent.generate();

  @BeforeEach
  public void setup(){
    testObject = TestDigitalObject.generate();
    testProject = testObject.getProject();
    projectRepository.save(testObject.getProject());
    digitalObjectRepository.save(testObject);

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

      Datastream datastream = TestDatastream.generate(testObject);

      datastreamService.save(datastream, TEST_MULTIPART_FILE);

      org.assertj.core.api.Assertions.assertThat(
          datastreamRepository.findById(datastream.deriveDatastreamId())
      ).isNotNull().isPresent();

      // cleanup
      datastreamRepository.delete(datastream);
      org.assertj.core.api.Assertions.assertThat(
          datastreamRepository.findById(datastream.deriveDatastreamId())
      ).isNotNull().isEmpty();
    }

    @Test
    public void savedDatastreamHasExpectedTagProperty(){
      Datastream datastream = TestDatastream.generate(testObject);
      Datastream savedDatastream = datastreamService.save(datastream, TEST_MULTIPART_FILE);
      org.assertj.core.api.Assertions.assertThat(savedDatastream)
          .isNotNull()
          .extracting(Datastream::getTags)
          .isEqualTo(datastream.getTags());

    }


  }

  @Nested
  public class DeleteDatastream {
    @Test
    public void successfullyDeletesDatastream() throws IOException {

      Datastream toBeDeleted = TestDatastream.generate(testObject);
      datastreamRepository.save(toBeDeleted);
      datastreamContentRepository.save(TEST_MULTIPART_FILE.getBytes(), toBeDeleted.deriveDatastreamId());

      // actual deletion
      datastreamService.delete(toBeDeleted);

      // check if datastream is deleted
      org.assertj.core.api.Assertions.assertThat(datastreamRepository.findById(toBeDeleted.deriveDatastreamId()))
          .isNotNull()
          .isEmpty();

      // check if datastream content is deleted
      org.assertj.core.api.Assertions.assertThat(datastreamContentRepository.exists(toBeDeleted.deriveDatastreamId()))
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
          .digitalObject(testObject)
          .baseMetadata(TestMetadataBaseEntity.generate())
          .build();

      Assertions.assertThrows(
          DatastreamNotFoundException.class,
          () -> datastreamService.delete(datastream)
      );
    }


  }

  @Nested
  public class FindAll {

    @Test
    public void returnsExpectedCountOfDatastreams(){

      Datastream datastream = TestDatastream.generate(testObject);
      Datastream datastream2 = TestDatastream.generate(testObject, "DSID2.txt");

      datastreamRepository.save(datastream);
      datastreamRepository.save(datastream2);

      org.assertj.core.api.Assertions.assertThat(datastreamService.findAll(testObject))
          .isNotNull()
          .isNotEmpty()
          .hasSize(2);

      // cleanup
      datastreamRepository.delete(datastream);
      datastreamRepository.delete(datastream2);

    }

  }

  @Nested
  public class FindById {

    @Test
    public void returnsExpectedDatastream(){

      Datastream datastream = TestDatastream.generate(testObject);

      datastreamRepository.save(datastream);

      org.assertj.core.api.Assertions.assertThat(datastreamService.findById(datastream.deriveDatastreamId()))
          .isNotNull()
          .isEqualTo(datastream);

      // cleanup
      datastreamRepository.delete(datastream);

    }

    @Test
    public void throwsIfDatastreamNotFound(){

      DatastreamId randomId = new DatastreamId("SOME_RANDOM_PID", "SOME_RANDOM_DSID");

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

      Datastream datastream = TestDatastream.generate(testObject);

      datastreamRepository.save(datastream);

      org.assertj.core.api.Assertions.assertThat(datastreamService.findDatastreamDetailsById(datastream.deriveDatastreamId()))
          .isNotNull()
          .extracting("dsid")
          .isEqualTo(datastream.getDsid());

      // cleanup
      datastreamRepository.delete(datastream);

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
