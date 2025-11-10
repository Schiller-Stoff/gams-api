package org.ddh.gamsapi.application.Integration.GSearch;

public enum BaseSearchTypes {

  DIGITAL_OBJECT("digitalObject"),
  DERIVATIVE("derivative");

  public final String name;

  BaseSearchTypes(String name){
    this.name = name;
  }
}
