package org.ddh.gamsapi.application.Integration.CustomSearch;

/**
 * Enum representing custom search entity properties.
 */
public enum CustomSearchProperties {

  DATASTREAM_DSID("CUSTOM_SEARCH.json"),
  ENTITY_ID("id"),
  ENTITY_PROJECT_ABBR("objectProjectAbbr"),
  SOLR_FULLTEXT_PROPERTY("entityFulltext"),
  ENTITY_OBJECT_ID("objectId"),
  ENTITY_TAGS("entityTags"),
  ENTITY_START_DATE("entityStartDate"),  // ✅ NEW
  ENTITY_END_DATE("entityEndDate");

  public final String name;

  CustomSearchProperties(String name) {
    this.name = name;
  }
}
