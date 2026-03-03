package org.ddh.gamsapi.application.Integration.GSearch;

public enum ApiSearchProperties {

  PROJECT("objectProjectAbbr"),

  OBJECT_ID("id"),
  DATASTREAMS("objectDatastreams"),

  TYPE("objectType"),

  FULLTEXT("objectFulltext"),

  TITLE("objectTitle"),

  DESCRIPTION("objectDesc"),

  CREATOR("objectCreator"),

  PUBLISHER("objectPublisher"),

  RIGHTS("objectRights"),

  TAGS("objectTags");


  public final String name;

  ApiSearchProperties(String name){
    this.name = name;
  }

}
