package org.zim.gamsapi.Datastream;

import org.junit.Assert;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.zim.gamsapi.Datastream.interfaces.IDatastreamService;
import org.zim.gamsapi.DigitalObject.DigitalObject;
import org.zim.gamsapi.DigitalObject.DigitalObjectBuilder;
import org.zim.gamsapi.DigitalObject.IDigitalObjectRepository;
import org.zim.gamsapi.DigitalObject.exceptions.DigitalObjectNotFoundException;
import org.zim.gamsapi.IntegrationTest;
import org.zim.gamsapi.Project.Project;
import org.zim.gamsapi.Project.interfaces.IProjectRepository;
import org.zim.gamsapi.enums.TestDatastream;
import org.zim.gamsapi.enums.TestDigitalObject;
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
          .build();
      datastreamRepository.save(toBeDeleted);
      // actual deletion
      datastreamService.delete(toBeDeleted);

      // check if datastream is deleted
      org.assertj.core.api.Assertions.assertThat(datastreamRepository.findById(toBeDeleted.deriveDatastreamId()))
          .isNotNull()
          .isEmpty();

    }


  }


}
