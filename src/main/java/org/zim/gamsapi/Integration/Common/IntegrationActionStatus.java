package org.zim.gamsapi.Integration.Common;

/**
 * Representing the status of an indexing operation per digital object via gams-api.
 * E.g. if indexing to solr succeeded etc.
 *
 */
public enum IntegrationActionStatus {

  /**
   * Indexing operation was successful
   */
  SUCCESS("success"),

  /**
   * Represents an error at an indexing operation.
   */
  ERROR("error"),

  /**
   * Skipped index operation e.g. if a datastream-id is not available.
   */
  SKIPPED("skipped");

  public final String name;

  IntegrationActionStatus(String name){
    this.name = name;
  }

}
