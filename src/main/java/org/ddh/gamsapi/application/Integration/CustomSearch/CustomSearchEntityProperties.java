package org.ddh.gamsapi.application.Integration.CustomSearch;

/**
 * Enum representing custom search entity properties.
 */
public enum CustomSearchEntityProperties {

  ENTITY_PROJECT_ABBR("objectProjectAbbr"),
  ENTITY_OBJECT_ID("objectId");

  public final String name;

  CustomSearchEntityProperties(String name) {
    this.name = name;
  }
}
