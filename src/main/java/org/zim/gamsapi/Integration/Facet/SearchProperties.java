package org.zim.gamsapi.Integration.Facet;

public enum SearchProperties {

  ID("id"),
  PROJECT("_projectAbbr"),
  OBJECT_ID("_id"),
  DATASTREAMS("_datastreams"),

  TYPE("_type"),

  FULLTEXT("_fulltext");

  public final String name;

  SearchProperties(String name){
    this.name = name;
  }

}
