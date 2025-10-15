package org.zim.gamsapi.application.Integration.BaseSearch;

public enum BaseSearchTypes {

  DIGITAL_OBJECT("digitalObject"),
  DERIVATIVE("derivative");

  public final String name;

  BaseSearchTypes(String name){
    this.name = name;
  }
}
