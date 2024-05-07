package org.zim.gamsapi.Integration.Common.enums;

/**
 * Represents the type of the integration action -- e.g. create object indices.
 */
public enum IntegrationActionType {

  /**
   * Represents a status where a digital object should be indexed.
   */
  INDEX_OBJECT("index-object"),

  /**
   * Represents a status where a digital object's indices should be removed.
   */
  DELETE_OBJECT("delete-object");


  public final String name;

  IntegrationActionType(String name){
    this.name = name;
  }

}
