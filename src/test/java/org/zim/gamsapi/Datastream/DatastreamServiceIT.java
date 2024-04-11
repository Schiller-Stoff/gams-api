package org.zim.gamsapi.Datastream;

import org.junit.Assert;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.zim.gamsapi.Datastream.interfaces.IDatastreamService;
import org.zim.gamsapi.DigitalObject.DigitalObject;
import org.zim.gamsapi.DigitalObject.IDigitalObjectRepository;
import org.zim.gamsapi.DigitalObject.exceptions.DigitalObjectNotFoundException;
import org.zim.gamsapi.IntegrationTest;
import org.zim.gamsapi.Project.Project;
import org.zim.gamsapi.Project.interfaces.IProjectRepository;
import org.zim.gamsapi.System.utils.DigitalObjectBuilder;
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
    testObject = new DigitalObjectBuilder(TestDigitalObject.DIGITAL_OBJECT_ID.getValue())
        .addProject(TestProject.PROJECT_ABBR.getValue())
        .add()
        .build();

    testProject = testObject.getProject();

    projectRepository.save(testObject.getProject());
    digitalObjectRepository.save(testObject);

  }

  @AfterAll
  public void tearDown(){
    digitalObjectRepository.delete(testObject);
    projectRepository.delete(testProject);
  }

  @Nested
  public class SaveDatastream {

    @Test
    public void throwsIfReferencedDigitalObjectNotFound(){

      final String RANDOM_PID = "SOME_RANDOM_PID";

      Datastream datastream = Datastream.builder()
          .dsid(TestDatastream.DSID.getValue())
          .digitalObject(
              new DigitalObjectBuilder(RANDOM_PID)
                  .addProject(TestProject.PROJECT_ABBR.getValue())
                  .add()
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
      Datastream datastream = Datastream.builder()
          .dsid(RANDOM_DSID)
          .digitalObject(testObject)
          .build();

      datastreamService.save(datastream);

      org.assertj.core.api.Assertions.assertThat(
          datastreamRepository.findByDigitalObjectAndDsid(testObject, datastream.getDsid())
      ).isNotNull().isPresent();

      // cleanup
      datastreamRepository.delete(datastream);
      org.assertj.core.api.Assertions.assertThat(
          datastreamRepository.findByDigitalObjectAndDsid(testObject, datastream.getDsid())
      ).isNotNull().isEmpty();

    }


  }


}
