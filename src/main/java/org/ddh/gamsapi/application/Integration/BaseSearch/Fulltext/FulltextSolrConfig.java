package org.ddh.gamsapi.application.Integration.BaseSearch.Fulltext;

/**
 * Enum for fulltext Solr configuration.
 */
public enum FulltextSolrConfig {

  /**
   * Suffix for fulltext fields in Solr schema.
   */
  DC_FIELD_FULLTEXT_SUFFIX("_txt"),

  /**
   * Highlighting pre and post tags for Solr response.
   */
  HIGHLIGHT_PRE("<mark>"),

  /**
   * Highlighting post tag for Solr response.
   */
  HIGHLIGHT_POST("</mark>");

  public final String name;

  FulltextSolrConfig(String name){
    this.name = name;
  }
}
