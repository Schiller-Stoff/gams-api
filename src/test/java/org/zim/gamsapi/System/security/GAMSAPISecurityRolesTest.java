package org.zim.gamsapi.System.security;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.zim.gamsapi.UnitTest;

public class GAMSAPISecurityRolesTest extends UnitTest {

  @Test
  public void testGetAdmin() {
    String expectedRole = "ROLE_admin";
    Assertions.assertEquals(expectedRole, GAMSAPISecurityRoles.getAdmin());
  }

  @Test
  public void testGetProjectAdmin() {
    String projectAbbr = "testProject";
    String expectedRole = "ROLE_testProject_admin";
    Assertions.assertEquals(expectedRole, GAMSAPISecurityRoles.getProjectAdmin(projectAbbr));
  }

  @Test
  public void testGetProjectEditor() {
    String projectAbbr = "testProject";
    String expectedRole = "ROLE_testProject_editor";
    Assertions.assertEquals(expectedRole, GAMSAPISecurityRoles.getProjectEditor(projectAbbr));
  }

  @Test
  public void testGetProjectViewer() {
    String projectAbbr = "testProject";
    String expectedRole = "ROLE_testProject_viewer";
    Assertions.assertEquals(expectedRole, GAMSAPISecurityRoles.getProjectViewer(projectAbbr));
  }

}
