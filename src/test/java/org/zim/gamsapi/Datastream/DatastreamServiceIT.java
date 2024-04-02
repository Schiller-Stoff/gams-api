package org.zim.gamsapi.Datastream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.zim.gamsapi.Datastream.interfaces.IDatastreamService;
import org.zim.gamsapi.DigitalObject.DigitalObject;
import org.zim.gamsapi.DigitalObject.exceptions.DigitalObjectNotFoundException;
import org.zim.gamsapi.IntegrationTest;
import org.zim.gamsapi.System.utils.DigitalObjectBuilder;
import org.zim.gamsapi.enums.TestDatastream;
import org.zim.gamsapi.enums.TestDigitalObject;
import org.zim.gamsapi.enums.TestProject;

public class DatastreamServiceIT extends IntegrationTest {

  @Autowired
  IDatastreamService datastreamService;

  @Nested
  public class SaveDatastream {

    @Test
    public void throwsIfReferencedDigitalObjectNotFound(){

      DigitalObject digitalObject = new DigitalObjectBuilder(TestDigitalObject.DIGITAL_OBJECT_ID.getValue())
          .addDatastream(TestDatastream.DSID.getValue())
          .add()
          .addProject(TestProject.PROJECT_ABBR.getValue())
          .add()
          .build();


      final Datastream datastream = digitalObject.getDatastreams().iterator().next();

      Assertions.assertThrows(
          DigitalObjectNotFoundException.class,
          () -> datastreamService.save(datastream)
      );


    }


  }


}
