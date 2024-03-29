package org.zim.gamsapi.Project;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.zim.gamsapi.DigitalObject.DigitalObject;
import org.zim.gamsapi.UnitTest;
import org.zim.gamsapi.enums.TestDigitalObject;
import org.zim.gamsapi.enums.TestProject;

public class ProjectTest extends UnitTest {


  @Test
  public void addDigitalObjectEstablishesBidirectionalRelationship(){

    DigitalObject digitalObject = new DigitalObject();
    //digitalObject.setId(TestDigitalObject.DIGITAL_OBJECT_ID.getValue());

    Project project = new Project();
    project.setProjectAbbr(TestProject.PROJECT_ABBR.getValue());

    project.addDigitalObject(digitalObject);

    // check equality
    Assertions.assertEquals(project, digitalObject.getProject());
    Assertions.assertEquals(project.getDigitalObjects().iterator().next(), digitalObject);

    //System.out.println("*****object: " + project);

    // check if the digital object is in the project
    Assertions.assertTrue(project.getDigitalObjects().contains(digitalObject));


  }


}
