package org.zim.gamsapi.System.security;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.zim.gamsapi.UnitTest;

public class GAMSAPIAuthoritiesTest extends UnitTest {

  @Test
  public void testGetAdmin() {
    String expectedRole = "ROLE_admin";
    Assertions.assertEquals(expectedRole, GAMSAPIAuthorities.getAdmin());
  }

  @Test
  public void testGetProjectAdmin() {
    String projectAbbr = "testProject";
    String expectedRole = "ROLE_testProject_admin";
    Assertions.assertEquals(expectedRole, GAMSAPIAuthorities.getProjectAdmin(projectAbbr));
  }

  @Test
  public void testGetProjectEditor() {
    String projectAbbr = "testProject";
    String expectedRole = "ROLE_testProject_editor";
    Assertions.assertEquals(expectedRole, GAMSAPIAuthorities.getProjectEditor(projectAbbr));
  }

  @Test
  public void testGetProjectViewer() {
    String projectAbbr = "testProject";
    String expectedRole = "ROLE_testProject_viewer";
    Assertions.assertEquals(expectedRole, GAMSAPIAuthorities.getProjectViewer(projectAbbr));
  }

  @Nested
  public class ExtractProjectAbbrFromAuthority {

    @Test
    public void extractProjectAbbrFromAuthorityReturnsCorrectAbbr() {
      String authority = "ROLE_testProject_admin";
      String expectedAbbr = "testProject";
      Assertions.assertEquals(expectedAbbr, GAMSAPIAuthorities.extractProjectAbbrFromAuthority(authority));
    }

    @Test
    public void extractProjectAbbrFromAuthorityReturnsNullWhenNoDelimiter() {
      String authority = "ROLE_admin";
      Assertions.assertNull(GAMSAPIAuthorities.extractProjectAbbrFromAuthority(authority));
    }

    @Test
    public void extractProjectAbbrFromAuthorityReturnsNullIfNoRolePrefix() {
      String authority = "testProject_admin";
      Assertions.assertNull(GAMSAPIAuthorities.extractProjectAbbrFromAuthority(authority));
    }

  }

  @Nested
  public class AuthorityMatchesProjectAbbr {

    @Test
    public void authorityMatchesProjectAbbrReturnsTrueWhenMatch() {
      String authority = "ROLE_testProject_admin";
      String projectAbbr = "testProject";
      Assertions.assertTrue(GAMSAPIAuthorities.authorityMatchesProjectAbbr(authority, projectAbbr));
    }

    @Test
    public void authorityMatchesProjectAbbrReturnsFalseWhenNoMatch() {
      String authority = "ROLE_testProject_admin";
      String projectAbbr = "otherProject";
      Assertions.assertFalse(GAMSAPIAuthorities.authorityMatchesProjectAbbr(authority, projectAbbr));
    }

    @Test
    public void authorityMatchesProjectAbbrReturnsFalseWhenNoDelimiter() {
      String authority = "ROLE_admin";
      String projectAbbr = "testProject";
      Assertions.assertFalse(GAMSAPIAuthorities.authorityMatchesProjectAbbr(authority, projectAbbr));
    }


  }

  @Nested
  public class ConvertToRole {

    @Test
    public void convertsExpectedAuthorityString(){

      final String AUTHORITY_TO_CONVERT = GAMSAPIAuthorities.getAdmin();
      final String EXPECTED = AUTHORITY_TO_CONVERT.replace(GAMSAPIAuthorities.ROLE_PREFIX.name, "");

      GAMSAPIAuthorities.convertToRole(AUTHORITY_TO_CONVERT);

      org.assertj.core.api.Assertions.assertThat(GAMSAPIAuthorities.convertToRole(AUTHORITY_TO_CONVERT))
          .isEqualTo(EXPECTED);

    }

    @Test
    public void throwsIfGivenAuthorityIsNotStartingWithRolePrefix(){
      Assertions.assertThrows(IllegalStateException.class, () -> {
        GAMSAPIAuthorities.convertToRole("admin");
      });
    }

    @Test
    public void throwsIfGivenAuthorityIsEmpty(){
      Assertions.assertThrows(IllegalStateException.class, () -> {
        GAMSAPIAuthorities.convertToRole("");
      });
    }

    @Test
    public void throwsIfGivenAuthorityIsNull(){
      Assertions.assertThrows(IllegalStateException.class, () -> {
        GAMSAPIAuthorities.convertToRole(null);
      });
    }


  }

}
