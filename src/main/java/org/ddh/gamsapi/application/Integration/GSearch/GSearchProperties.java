package org.ddh.gamsapi.application.Integration.GSearch;

public enum GSearchProperties {

  PROJECT("objectProjectAbbr"),

  OBJECT_ID("id"),
  DATASTREAMS("objectDatastreams"),

  TYPE("objectType"),

  FULLTEXT("objectFulltext"),

  TITLE("objectTitle"),

  DESCRIPTION("objectDesc"),

  CREATOR("objectCreator"),

  PUBLISHER("objectPublisher"),

  RIGHTS("objectRights");


  public final String name;

  GSearchProperties(String name){
    this.name = name;
  }

}
