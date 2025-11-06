package org.ddh.gamsapi.application.Integration.Common.utils.solr;

/**
 * Enum of Solr cores used in GAMS.
 */
public enum SolrGamsCores {

  GAMS_CORE("gams"),
  TEST_CORE("test"),
  CUSTOM_SEARCH_CORE("custom-search"),
  BASE_CORE_CONFIG("base");

  public final String value;

  SolrGamsCores(String value) {
    this.value = value;
  }

}
