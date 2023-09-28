package org.zim.gamsapi.Integration.Facet;

public enum SearchTypes {

  DIGITAL_OBJECT("digitalObject"),
  DERIVATIVE("derivative");

  public final String name;

  SearchTypes(String name){
    this.name = name;
  }
}
