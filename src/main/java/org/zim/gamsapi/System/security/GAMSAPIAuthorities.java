package org.zim.gamsapi.System.security;

import lombok.extern.slf4j.Slf4j;
import org.zim.gamsapi.System.security.exceptions.AuthorizationConfigurationException;

/**
 * Represents the different roles available in GAMS5 context.
 */
@Slf4j
public enum GAMSAPIAuthorities {

  ADMINISTRATOR("admin"),

  ANONYMOUS("anonymous"),

  PROJECT_ADMINISTRATOR("admin"),

  PROJECT_EDITOR("editor"),

  PROJECT_VIEWER("viewer"),

  ROLE_DELIMITER("_"),

  ROLE_PREFIX("ROLE" + ROLE_DELIMITER.name);

  public final String name;

  GAMSAPIAuthorities(String name){
    this.name = name;
  }


  /**
   * Returns the full admin role
   * @return the full admin role
   */
  public static String getAdmin() {
    return GAMSAPIAuthorities.ROLE_PREFIX.name + GAMSAPIAuthorities.ADMINISTRATOR.name;
  }

  /**
   * Returns the full anonymous role
   * @return the full anonymous role
   */
  public static String getAnonymous() {
    return GAMSAPIAuthorities.ROLE_PREFIX.name + GAMSAPIAuthorities.ANONYMOUS.name;
  }

  /**
   * Returns the full project admin role
   * @param projectAbbr the project abbreviation
   * @return the full project admin role
   */
  public static String getProjectAdmin(String projectAbbr) {
    return GAMSAPIAuthorities.ROLE_PREFIX.name + projectAbbr + GAMSAPIAuthorities.ROLE_DELIMITER.name + GAMSAPIAuthorities.PROJECT_ADMINISTRATOR.name;
  }

  /**
   * Returns the full project editor role
   * @param projectAbbr the project abbreviation
   * @return the full project editor role
   */
  public static String getProjectEditor(String projectAbbr) {
    return GAMSAPIAuthorities.ROLE_PREFIX.name + projectAbbr + GAMSAPIAuthorities.ROLE_DELIMITER.name + GAMSAPIAuthorities.PROJECT_EDITOR.name;
  }

  /**
   * Returns the full project viewer role
   * @param projectAbbr the project abbreviation
   * @return the full project viewer role
   */
  public static String getProjectViewer(String projectAbbr) {
    return GAMSAPIAuthorities.ROLE_PREFIX.name + projectAbbr + GAMSAPIAuthorities.ROLE_DELIMITER.name + GAMSAPIAuthorities.PROJECT_VIEWER.name;
  }

  /**
   * Extracts the project abbreviation from the given authority
   * @param authority the authority
   * @return the project abbreviation or null if not found
   */
  public static String extractProjectAbbrFromAuthority(String authority) throws AuthorizationConfigurationException {
    if(!authority.contains(GAMSAPIAuthorities.ROLE_PREFIX.name)) {
      String msg = String.format("Authority %s does not contain the role prefix %s. Every authority should have ben mapped to the role prefix handled by this app. Cannot extract project abbreviation.", authority, GAMSAPIAuthorities.ROLE_PREFIX.name);
      log.trace(msg);
      return null;
    }
    // remove the ROLE_ prefix
    authority = authority.replace(GAMSAPIAuthorities.ROLE_PREFIX.name, "");

    int delimiterIndex = authority.indexOf(GAMSAPIAuthorities.ROLE_DELIMITER.name);
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
    String authorityProjectAbbr = GAMSAPIAuthorities.extractProjectAbbrFromAuthority(authority);
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
    return GAMSAPIAuthorities.getProjectViewer(projectAbbr) + GAMSAPIAuthorities.ROLE_DELIMITER.name + contentRestriction;
  }

  /**
   * Converts a given authority to a role by removing the ROLE_ prefix
   * TODO test
   * @param authority the authority
   * @return the role (authority WITHOUT ROLE_ prefix)
   */
  public static String convertToRole(String authority){
    if(!authority.startsWith(GAMSAPIAuthorities.ROLE_PREFIX.name)){
      String msg = String.format("Authority %s has no role prefix %s. Cannot convert to role (because it is already a role?).", authority, GAMSAPIAuthorities.ROLE_PREFIX.name);
      log.error(msg);
      throw new IllegalStateException(msg);
    }
    // throw if null or empty
    if(authority.isEmpty()){
      String msg = "Authority is unexpectedly null. Cannot convert to role.";
      log.error(msg);
      throw new IllegalStateException(msg);
    }

    return authority.replace(GAMSAPIAuthorities.ROLE_PREFIX.name, "");
  }

}
