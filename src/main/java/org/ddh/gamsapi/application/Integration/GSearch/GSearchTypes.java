package org.ddh.gamsapi.application.Integration.GSearch;

public enum GSearchTypes {

  DIGITAL_OBJECT("digitalObject"),
  DERIVATIVE("derivative");

  public final String name;

  GSearchTypes(String name){
    this.name = name;
  }
}
