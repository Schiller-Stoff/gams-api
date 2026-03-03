package org.ddh.gamsapi.application.Integration.GSearch;

public enum ApiSearchTypes {

  DIGITAL_OBJECT("digitalObject"),
  DERIVATIVE("derivative");

  public final String name;

  ApiSearchTypes(String name){
    this.name = name;
  }
}
