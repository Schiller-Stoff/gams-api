package org.zim.gamsapi.Integration.BaseSearch;

public enum BaseSearchProperties {

  ID("id"),
  PROJECT("_projectAbbr"),
  OBJECT_ID("_id"),
  DATASTREAMS("_datastreams"),

  TYPE("_type"),

  FULLTEXT("_fulltext");

  public final String name;

  BaseSearchProperties(String name){
    this.name = name;
  }

}
