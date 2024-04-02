package org.zim.gamsapi.Project;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.zim.gamsapi.DigitalObject.DigitalObject;
import org.zim.gamsapi.UnitTest;
import org.zim.gamsapi.enums.TestProject;

public class ProjectTest extends UnitTest {


  @Test
  public void addDigitalObjectEstablishesBidirectionalRelationship(){

    DigitalObject digitalObject = new DigitalObject();

    Project project = new Project();
    project.setProjectAbbr(TestProject.PROJECT_ABBR.getValue());

    project.addDigitalObject(digitalObject);

    // check equality
    Assertions.assertEquals(project, digitalObject.getProject());
    Assertions.assertEquals(project.getDigitalObjects().iterator().next(), digitalObject);

    // check if the digital object is in the project
    Assertions.assertTrue(project.getDigitalObjects().contains(digitalObject));


  }

  @Test
  public void removeDigitalObjectRemovesBidirectionalRelationship(){

    DigitalObject digitalObject = new DigitalObject();

    Project project = new Project();
    project.setProjectAbbr(TestProject.PROJECT_ABBR.getValue());

    project.addDigitalObject(digitalObject);

    // check equality
    Assertions.assertEquals(project, digitalObject.getProject());
    Assertions.assertEquals(project.getDigitalObjects().iterator().next(), digitalObject);

    // check if the digital object is in the project
    Assertions.assertTrue(project.getDigitalObjects().contains(digitalObject));

    project.removeDigitalObject(digitalObject);

    // check if the digital object is not in the project
    Assertions.assertFalse(project.getDigitalObjects().contains(digitalObject));

  }


}
