package org.ddh.gamsapi.application.Integration.BaseSearch.Fulltext;

/**
 * Enum for fulltext Solr configuration.
 */
public enum FulltextSolrConfig {

  /**
   * Suffix for phrase search fields in Solr schema.
   *
   */
  PHRASE_SEARCH_SUFFIX("AsPhrase"),

  /**
   * Suffix for fulltext fields in Solr schema.
   */
  DC_FIELD_FULLTEXT_SUFFIX("_txt");

  public final String name;

  FulltextSolrConfig(String name){
    this.name = name;
  }
}
