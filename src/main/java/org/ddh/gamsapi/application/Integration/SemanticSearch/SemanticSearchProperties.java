package org.ddh.gamsapi.application.Integration.SemanticSearch;

/**
 * Enum representing semantic search entity properties.
 */
public enum SemanticSearchProperties {

  DATASTREAM_DSID("SEMANTIC_STATEMENTS.ttl");

  public final String name;

  SemanticSearchProperties(String name) {
    this.name = name;
  }
}
