package org.ddh.gamsapi.application.Integration.ApiSearch.Fulltext;

/**
 * Enum for fulltext response properties.
 */
public enum FulltextRequestProperties {

  /**
   * Highlighting property in API response.
   */
  HIGHLIGHTING("highlighting"),


  /**
   * Suffix for phrase search fields in Solr schema.
   *
   */
  PHRASE_SEARCH_SUFFIX("AsPhrase");

  public final String name;

  FulltextRequestProperties(String name){
    this.name = name;
  }

}
