package org.zim.gamsapi.Project;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.zim.gamsapi.UnitTest;

public class ProjectTest extends UnitTest {


  @Nested
  public class IdentityTest {

    @Test
    public void twoProjectsWithSameProjectAbbr_aresConsideredEqual() {

      // given
      final String PROJECT_ABBR = "projectAbbr";

      Project project1 = Project.builder().projectAbbr(PROJECT_ABBR).build();
      Project project2 = Project.builder().projectAbbr(PROJECT_ABBR).build();

      // then
      Assertions.assertEquals(project1, project2);

    }


  }


}
