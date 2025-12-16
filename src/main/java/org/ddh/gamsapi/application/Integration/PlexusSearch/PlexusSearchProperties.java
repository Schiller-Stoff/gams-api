package org.ddh.gamsapi.application.Integration.PlexusSearch;

/**
 * Enum representing plexus search entity properties.
 */
public enum PlexusSearchProperties {

  DATASTREAM_DSID("CUSTOM_PLEXUS_SEARCH.json"),
  ENTITY_ID("id"),
  ENTITY_PROJECT_ABBR("objectProjectAbbr"),
  ENTITY_OBJECT_ID("objectId");

  public final String name;

  PlexusSearchProperties(String name) {
    this.name = name;
  }
}
