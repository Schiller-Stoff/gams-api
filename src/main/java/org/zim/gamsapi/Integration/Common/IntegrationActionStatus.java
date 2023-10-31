package org.zim.gamsapi.Integration.Common;

public enum IntegrationActionStatus {

  SUCCESS("success"),

  ERROR("error");

  public final String name;

  IntegrationActionStatus(String name){
    this.name = name;
  }

}
