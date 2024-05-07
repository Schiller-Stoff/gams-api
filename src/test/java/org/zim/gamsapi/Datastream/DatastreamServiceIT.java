package org.zim.gamsapi.Datastream;

import org.junit.Assert;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.zim.gamsapi.Datastream.exceptions.DatastreamNotFoundException;
import org.zim.gamsapi.Datastream.interfaces.IDatastreamService;
import org.zim.gamsapi.DigitalObject.DigitalObject;
import org.zim.gamsapi.DigitalObject.DigitalObjectBuilder;
import org.zim.gamsapi.DigitalObject.IDigitalObjectRepository;
import org.zim.gamsapi.DigitalObject.exceptions.DigitalObjectNotFoundException;
import org.zim.gamsapi.IntegrationTest;
import org.zim.gamsapi.MetadataBaseEntityBuilder;
import org.zim.gamsapi.Project.Project;
import org.zim.gamsapi.Project.interfaces.IProjectRepository;
import org.zim.gamsapi.enums.TestDatastream;
import org.zim.gamsapi.enums.TestDigitalObject;
import org.zim.gamsapi.enums.TestMetadataBaseEntity;
import org.zim.gamsapi.enums.TestProject;

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

  private DigitalObject testObject;

  private Project testProject;

  @BeforeAll
  public void setup(){
    testObject = new DigitalObjectBuilder()
        .id(TestDigitalObject.DIGITAL_OBJECT_ID.getValue())
        .project(Project.builder().projectAbbr(TestProject.PROJECT_ABBR.getValue()).build())
        .baseMetadata(TestMetadataBaseEntity.generate())
        .build();

    testProject = testObject.getProject();

    projectRepository.save(testObject.getProject());
    digitalObjectRepository.save(testObject);

  }

  @AfterAll
  public void tearDown(){
    digitalObjectRepository.delete(testObject);
    projectRepository.delete(testProject);

    // check that nothing exists after cleanup
    org.assertj.core.api.Assertions.assertThat(digitalObjectRepository.findAll())
        .isNotNull()
        .isEmpty();

    org.assertj.core.api.Assertions.assertThat(datastreamRepository.findAll())
        .isNotNull()
        .isEmpty();

    org.assertj.core.api.Assertions.assertThat(projectRepository.findAll())
        .isNotNull()
        .isEmpty();

  }

  @Nested
  public class SaveDatastream {

    @Test
    public void throwsIfReferencedDigitalObjectNotFound(){

      final String RANDOM_PID = "SOME_RANDOM_PID";

      Datastream datastream = new DatastreamBuilder()
          .dsid(TestDatastream.DSID.getValue())
          .digitalObject(
              new DigitalObjectBuilder()
                  .id(RANDOM_PID)
                  .project(Project.builder().projectAbbr(TestProject.PROJECT_ABBR.getValue()).build())
                  .baseMetadata(TestMetadataBaseEntity.generate())
                  .build()
          )
          .build();

      Assertions.assertThrows(
          DigitalObjectNotFoundException.class,
          () -> datastreamService.save(datastream)
      );

    }

    @Test
    public void datastreamExistsAfterSaving(){

      final String RANDOM_DSID = "SOME_RANDOM_DSID";
      Datastream datastream = new DatastreamBuilder()
          .dsid(RANDOM_DSID)
          .digitalObject(testObject)
          .baseMetadata(TestMetadataBaseEntity.generate())
          .build();

      datastreamService.save(datastream);

      org.assertj.core.api.Assertions.assertThat(
          datastreamRepository.findById(datastream.deriveDatastreamId())
      ).isNotNull().isPresent();

      // cleanup
      datastreamRepository.delete(datastream);
      org.assertj.core.api.Assertions.assertThat(
          datastreamRepository.findById(datastream.deriveDatastreamId())
      ).isNotNull().isEmpty();


    }


  }

  @Nested
  public class DeleteDatastream {
    @Test
    public void successfullyDeletesDatastream(){

      Datastream toBeDeleted = new DatastreamBuilder()
          .dsid(TestDatastream.DSID.getValue())
          .digitalObject(testObject)
          .baseMetadata(TestMetadataBaseEntity.generate())
          .build();
      datastreamRepository.save(toBeDeleted);
      // actual deletion
      datastreamService.delete(toBeDeleted);

      // check if datastream is deleted
      org.assertj.core.api.Assertions.assertThat(datastreamRepository.findById(toBeDeleted.deriveDatastreamId()))
          .isNotNull()
          .isEmpty();

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

      Datastream datastream = new DatastreamBuilder()
          .dsid(TestDatastream.DSID.getValue())
          .digitalObject(testObject)
          .baseMetadata(TestMetadataBaseEntity.generate())
          .build();

      Datastream datastream2 = new DatastreamBuilder()
          .dsid("DSID2")
          .digitalObject(testObject)
          .baseMetadata(TestMetadataBaseEntity.generate())
          .build();

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

      Datastream datastream = new DatastreamBuilder()
          .dsid(TestDatastream.DSID.getValue())
          .digitalObject(testObject)
          .baseMetadata(TestMetadataBaseEntity.generate())
          .build();

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

      Datastream datastream = new DatastreamBuilder()
          .dsid(TestDatastream.DSID.getValue())
          .digitalObject(testObject)
          .baseMetadata(TestMetadataBaseEntity.generate())
          .build();

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
