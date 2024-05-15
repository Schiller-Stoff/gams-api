package org.zim.gamsapi.System.security;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.zim.gamsapi.System.security.exceptions.AuthorizationConfigurationException;
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

  @Nested
  public class ExtractProjectAbbrFromAuthority {

    @Test
    public void extractProjectAbbrFromAuthorityReturnsCorrectAbbr() {
      String authority = "ROLE_testProject_admin";
      String expectedAbbr = "testProject";
      Assertions.assertEquals(expectedAbbr, GAMSAPISecurityRoles.extractProjectAbbrFromAuthority(authority));
    }

    @Test
    public void extractProjectAbbrFromAuthorityReturnsNullWhenNoDelimiter() {
      String authority = "ROLE_admin";
      Assertions.assertNull(GAMSAPISecurityRoles.extractProjectAbbrFromAuthority(authority));
    }

    @Test
    public void extractProjectAbbrFromAuthorityThrowsExceptionWhenNoRolePrefix() {
      String authority = "testProject_admin";
      Assertions.assertThrows(AuthorizationConfigurationException.class, () -> GAMSAPISecurityRoles.extractProjectAbbrFromAuthority(authority));
    }

  }

  @Nested
  public class AuthorityMatchesProjectAbbr {

    @Test
    public void authorityMatchesProjectAbbrReturnsTrueWhenMatch() {
      String authority = "ROLE_testProject_admin";
      String projectAbbr = "testProject";
      Assertions.assertTrue(GAMSAPISecurityRoles.authorityMatchesProjectAbbr(authority, projectAbbr));
    }

    @Test
    public void authorityMatchesProjectAbbrReturnsFalseWhenNoMatch() {
      String authority = "ROLE_testProject_admin";
      String projectAbbr = "otherProject";
      Assertions.assertFalse(GAMSAPISecurityRoles.authorityMatchesProjectAbbr(authority, projectAbbr));
    }

    @Test
    public void authorityMatchesProjectAbbrReturnsFalseWhenNoDelimiter() {
      String authority = "ROLE_admin";
      String projectAbbr = "testProject";
      Assertions.assertFalse(GAMSAPISecurityRoles.authorityMatchesProjectAbbr(authority, projectAbbr));
    }


  }

}
