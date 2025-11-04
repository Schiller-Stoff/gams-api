package org.ddh.gamsapi.application.Integration.BaseSearch.solr;

/**
 * Enum of Solr cores used in GAMS.
 */
public enum SolrGamsCores {

  GAMS_CORE("gams"),
  TEST_CORE("test"),
  FULLTEXT_CORE("fulltext"),
  BASE_CORE_CONFIG("base");

  public final String value;

  SolrGamsCores(String value) {
    this.value = value;
  }

}
