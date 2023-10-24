package org.zim.gamsapi.System.security;

/**
 * Represents the different roles available in GAMS5 context.
 */
public enum GAMSAPISecurityRoles {

  ADMINISTRATOR("ROLE_administrator"),
  EDITOR("ROLE_editor");

  public final String name;

  GAMSAPISecurityRoles(String name){
    this.name = name;
  }

}
