package org.ddh.gamsapi.application.Integration.Common.utils.solr;

/**
 * Enum of Solr cores used in GAMS.
 */
public enum SolrGamsCores {

  /**
   * Core name for the api search service
   */
  API_SEARCH_CORE("api-search"),
  TEST_CORE("test"),
  CUSTOM_SEARCH_CORE("custom-search"),
  PLEXUS_SEARCH_CORE("plexus-search"),
  BASE_CORE_CONFIG("base");

  public final String value;

  SolrGamsCores(String value) {
    this.value = value;
  }

}
