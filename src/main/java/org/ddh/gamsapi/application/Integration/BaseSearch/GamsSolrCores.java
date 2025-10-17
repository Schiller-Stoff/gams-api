package org.ddh.gamsapi.application.Integration.BaseSearch;

/**
 * Enum of Solr cores used in GAMS.
 */
public enum GamsSolrCores {

  GAMS_CORE("gams"),
  TEST_CORE("test"),
  BASE_CORE_CONFIG("base");

  public final String value;

  GamsSolrCores(String value) {
    this.value = value;
  }

}
