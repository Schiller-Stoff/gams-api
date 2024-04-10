package org.zim.gamsapi.Datastream;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.zim.gamsapi.Datastream.interfaces.IDatastreamService;
import org.zim.gamsapi.DigitalObject.DigitalObject;
import org.zim.gamsapi.DigitalObject.IDigitalObjectRepository;
import org.zim.gamsapi.DigitalObject.exceptions.DigitalObjectNotFoundException;
import org.zim.gamsapi.IntegrationTest;
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

  @BeforeAll
  public void setup(){
    DigitalObject testObject = new DigitalObjectBuilder(TestDigitalObject.DIGITAL_OBJECT_ID.getValue())
        .addProject(TestProject.PROJECT_ABBR.getValue())
        .add()
        .build();

    projectRepository.save(testObject.getProject());

    digitalObjectRepository.save(testObject);


  }

  @AfterAll
  public void tearDown(){
    digitalObjectRepository.deleteAll();
  }

  @Nested
  public class SaveDatastream {

    @Test
    public void throwsIfReferencedDigitalObjectNotFound(){

      final String RANDOM_PID = "SOME_RANDOM_PID";

      DigitalObject digitalObject = new DigitalObjectBuilder(RANDOM_PID)
          .addDatastream(TestDatastream.DSID.getValue())
          .add()
          .addProject(TestProject.PROJECT_ABBR.getValue())
          .add()
          .build();

      Assertions.fail("Test needs to be updated to reflect the new implementation");

//      final Datastream datastream = digitalObject.getDatastreams().iterator().next();
//
//      Assertions.assertThrows(
//          DigitalObjectNotFoundException.class,
//          () -> datastreamService.save(datastream)
//      );
//
//      digitalObjectRepository.delete(digitalObject);
    }

    @Test
    public void datastreamExistsAfterSaving(){

      final String RANDOM_DSID = "SOME_RANDOM_DSID";
      Datastream datastream = Datastream.builder()
          .dsid(RANDOM_DSID)
          .digitalObject(
              new DigitalObjectBuilder(TestDigitalObject.DIGITAL_OBJECT_ID.getValue())
                  .addProject(TestProject.PROJECT_ABBR.getValue())
                  .add()
                  .build()
          )
          .build();

      datastreamService.save(datastream);

      org.assertj.core.api.Assertions.assertThat(
          datastreamService.findByDsid(TestDigitalObject.DIGITAL_OBJECT_ID.getValue(), RANDOM_DSID)
      ).isNotNull();

    }


  }


}
