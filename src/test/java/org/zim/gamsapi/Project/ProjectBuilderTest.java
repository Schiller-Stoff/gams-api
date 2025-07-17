package org.zim.gamsapi.Project;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.zim.gamsapi.UnitTest;
import org.zim.gamsapi.TestUtilities.TestProject;

public class ProjectBuilderTest extends UnitTest {

  @Test
  public void throwsIfProjectAbbrIsNotSet(){
    Assertions.assertThrows(IllegalStateException.class, () -> {
      new ProjectBuilder().build();
    });
  }

  @Test
  public void buildsExpectedProject(){
    final String PROJECT_ABBR = TestProject.PROJECT_ABBR.getValue();
    final String PROJECT_DESCRIPTION = TestProject.PROJECT_DESCRIPTION.getValue();
    final String PROJECT_TITLE = TestProject.PROJECT_TITLE.getValue();

    Project project = new ProjectBuilder()
        .projectAbbr(PROJECT_ABBR)
        .description(PROJECT_DESCRIPTION)
        .title(PROJECT_TITLE)
        .build();
    // set
    Assertions.assertEquals(PROJECT_DESCRIPTION, project.getDescription());
    Assertions.assertEquals(PROJECT_ABBR, project.getProjectAbbr());
    Assertions.assertEquals(PROJECT_TITLE, project.getTitle());

    // should be null
    Assertions.assertNull(project.getCreatedBy());
    Assertions.assertNull(project.getCreated());
    Assertions.assertNull(project.getModifiedBy());
    Assertions.assertNull(project.getModified());
  }

}
