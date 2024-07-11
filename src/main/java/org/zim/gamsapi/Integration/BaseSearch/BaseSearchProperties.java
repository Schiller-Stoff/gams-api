package org.zim.gamsapi.Integration.BaseSearch;

public enum BaseSearchProperties {

  ID("id"),
  PROJECT("_projectAbbr"),
  OBJECT_ID("_id"),
  DATASTREAMS("_datastreams"),

  TYPE("_type"),

  FULLTEXT("_fulltext"),

  TITLE("_title"),

  DESCRIPTION("_description"),

  CREATOR("_creator"),

  PUBLISHER("_publisher"),

  RIGHTS("_rights");


  public final String name;

  BaseSearchProperties(String name){
    this.name = name;
  }

}
