package org.zim.gamsapi.System.configproperties;


/**
 * Contains basic hardcoded system properties, like
 * the static abbreviation of the demo project.
 */
public enum GAMSAPIProperties {

  ADMIN_USER_NAME("admin"),

  DEMO_PROJECT_ABBR("demo");

  public final String name;
  GAMSAPIProperties(String name){
    this.name = name;
  }

}
