package org.zim.gamsapi.Integration.Common;

public enum IntegrationActionType {

  INDEX_OBJECT("index-object"),

  DELETE_OBJECT("delete-object");


  public final String name;

  IntegrationActionType(String name){
    this.name = name;
  }

}
