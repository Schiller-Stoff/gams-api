package org.zim.gamsapi.System.security;

import lombok.extern.slf4j.Slf4j;
import org.zim.gamsapi.System.security.exceptions.AuthorizationConfigurationException;

/**
 * Represents the different roles available in GAMS5 context.
 */
@Slf4j
public enum GAMSAPISecurityRoles {

  ADMINISTRATOR("admin"),

  ANONYMOUS("anonymous"),

  PROJECT_ADMINISTRATOR("admin"),

  PROJECT_EDITOR("editor"),

  PROJECT_VIEWER("viewer"),

  ROLE_DELIMITER("_"),

  ROLE_PREFIX("ROLE" + ROLE_DELIMITER.name);

  public final String name;

  GAMSAPISecurityRoles(String name){
    this.name = name;
  }


  /**
   * Returns the full admin role
   * @return the full admin role
   */
  public static String getAdmin() {
    return GAMSAPISecurityRoles.ROLE_PREFIX.name + GAMSAPISecurityRoles.ADMINISTRATOR.name;
  }

  /**
   * Returns the full anonymous role
   * @return the full anonymous role
   */
  public static String getAnonymous() {
    return GAMSAPISecurityRoles.ROLE_PREFIX.name + GAMSAPISecurityRoles.ANONYMOUS.name;
  }

  /**
   * Returns the full project admin role
   * @param projectAbbr the project abbreviation
   * @return the full project admin role
   */
  public static String getProjectAdmin(String projectAbbr) {
    return GAMSAPISecurityRoles.ROLE_PREFIX.name + projectAbbr + GAMSAPISecurityRoles.ROLE_DELIMITER.name + GAMSAPISecurityRoles.PROJECT_ADMINISTRATOR.name;
  }

  /**
   * Returns the full project editor role
   * @param projectAbbr the project abbreviation
   * @return the full project editor role
   */
  public static String getProjectEditor(String projectAbbr) {
    return GAMSAPISecurityRoles.ROLE_PREFIX.name + projectAbbr + GAMSAPISecurityRoles.ROLE_DELIMITER.name + GAMSAPISecurityRoles.PROJECT_EDITOR.name;
  }

  /**
   * Returns the full project viewer role
   * @param projectAbbr the project abbreviation
   * @return the full project viewer role
   */
  public static String getProjectViewer(String projectAbbr) {
    return GAMSAPISecurityRoles.ROLE_PREFIX.name + projectAbbr + GAMSAPISecurityRoles.ROLE_DELIMITER.name + GAMSAPISecurityRoles.PROJECT_VIEWER.name;
  }

  /**
   * Extracts the project abbreviation from the given authority
   * @param authority the authority
   * @return the project abbreviation or null if not found
   */
  public static String extractProjectAbbrFromAuthority(String authority) throws AuthorizationConfigurationException {
    if(!authority.contains(GAMSAPISecurityRoles.ROLE_PREFIX.name)) {
      String msg = String.format("Authority %s does not contain the role prefix %s. Every authority should have ben mapped to the role prefix handled by this app. Cannot extract project abbreviation.", authority, GAMSAPISecurityRoles.ROLE_PREFIX.name);
      log.error(msg);
      throw new AuthorizationConfigurationException(msg);
    }
    // remove the ROLE_ prefix
    authority = authority.replace(GAMSAPISecurityRoles.ROLE_PREFIX.name, "");

    int delimiterIndex = authority.indexOf(GAMSAPISecurityRoles.ROLE_DELIMITER.name);
    if(delimiterIndex == -1) {
      return null;
    } else {
      return authority.substring(0, delimiterIndex);
    }

  }

  /**
   * Checks if the given authority matches the given project abbreviation (after the second delimiter
   * @param authority the authority
   * @param projectAbbr the project abbreviation
   * @return true if the authority matches the project abbreviation
   */
  public static boolean authorityMatchesProjectAbbr(String authority, String projectAbbr) {
    String authorityProjectAbbr = GAMSAPISecurityRoles.extractProjectAbbrFromAuthority(authority);
    if(authorityProjectAbbr == null) {
      return false;
    }
    return authorityProjectAbbr.equals(projectAbbr);
  }


  /**
   * Builds a project viewer restricted role
   * TODO test
   * @param projectAbbr the project abbreviation
   * @param contentRestriction the content restriction
   * @return the project viewer restricted role
   */
  public static String buildProjectViewerContentRestricted(String projectAbbr, String contentRestriction) {
    return GAMSAPISecurityRoles.getProjectViewer(projectAbbr) + GAMSAPISecurityRoles.ROLE_DELIMITER.name + contentRestriction;
  }

}
