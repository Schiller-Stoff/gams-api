package org.ddh.gamsapi.application.Integration.CustomSearch;

/**
 * Enum representing custom search entity properties.
 */
public enum CustomSearchProperties {

  DATASTREAM_DSID("CUSTOM_SEARCH.json"),
  ENTITY_ID("id"),
  ENTITY_PROJECT_ABBR("objectProjectAbbr"),
  ENTITY_OBJECT_ID("objectId");

  public final String name;

  CustomSearchProperties(String name) {
    this.name = name;
  }
}
