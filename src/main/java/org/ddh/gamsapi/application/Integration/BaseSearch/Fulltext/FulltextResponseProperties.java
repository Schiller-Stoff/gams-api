package org.ddh.gamsapi.application.Integration.BaseSearch.Fulltext;

/**
 * Enum for fulltext response properties.
 */
public enum FulltextResponseProperties {

  /**
   * Highlighting property in API response.
   */
  HIGHLIGHTING("highlighting"),
  HIGHLIGHT_PRE("<mark>"),
  HIGHLIGHT_POST("</mark>");

  public final String name;

  FulltextResponseProperties(String name){
    this.name = name;
  }

}
