package org.zim.gamsapi.application.Integration.BaseSearch;

public enum BaseSearchProperties {

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

  BaseSearchProperties(String name){
    this.name = name;
  }

}
