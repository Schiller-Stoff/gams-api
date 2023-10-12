package org.zim.gamsapi.System.security;

/**
 * Represents the different roles available in GAMS5 context.
 */
public enum SecurityRoles {

  ADMINISTRATOR("administrator"),
  EDITOR("editor");

  public final String name;

  SecurityRoles(String name){
    this.name = name;
  }

}
