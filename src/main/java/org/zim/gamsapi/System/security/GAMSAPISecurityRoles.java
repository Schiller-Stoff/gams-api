package org.zim.gamsapi.System.security;

/**
 * Represents the different roles available in GAMS5 context.
 */
public enum GAMSAPISecurityRoles {

  //  TODO remove public access to properties -> let things be built via getters.

  ADMINISTRATOR("admin"),

  // TODO remove this role?
  EDITOR("editor"),

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
   * Returns the full editor role
   * @return the full editor role
   */
  public static String getEditor() {
    return GAMSAPISecurityRoles.ROLE_PREFIX.name + GAMSAPISecurityRoles.EDITOR.name;
  }

  /**
   * Returns the full project admin role
   * TODO test
   * @param projectAbbr the project abbreviation
   * @return the full project admin role
   */
  public static String getProjectAdmin(String projectAbbr) {
    return GAMSAPISecurityRoles.ROLE_PREFIX.name + projectAbbr + GAMSAPISecurityRoles.ROLE_DELIMITER.name + GAMSAPISecurityRoles.PROJECT_ADMINISTRATOR.name;
  }

  /**
   * TODO test
   * Returns the full project editor role
   * @param projectAbbr the project abbreviation
   * @return the full project editor role
   */
  public static String getProjectEditor(String projectAbbr) {
    return GAMSAPISecurityRoles.ROLE_PREFIX.name + projectAbbr + GAMSAPISecurityRoles.ROLE_DELIMITER.name + GAMSAPISecurityRoles.PROJECT_EDITOR.name;
  }

  /**
   * TODO test
   * Returns the full project viewer role
   * @param projectAbbr the project abbreviation
   * @return the full project viewer role
   */
  public static String getProjectViewer(String projectAbbr) {
    return GAMSAPISecurityRoles.ROLE_PREFIX.name + projectAbbr + GAMSAPISecurityRoles.ROLE_DELIMITER.name + GAMSAPISecurityRoles.PROJECT_VIEWER.name;
  }

}
