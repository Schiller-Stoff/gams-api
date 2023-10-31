package org.zim.gamsapi.Integration.Common.enums;

/**
 * Contains specific datastream ids for gams integration.
 */
public enum GAMSAPIntegrationDatastreamId {

  /**
   * Datastream id of the source datastream.
   */
  SOURCE_DATASTREAM_ID("source_xml"),

  /**
   * Datastream id of defined rdf datastream.
   */
  RDF_DATASTREAM_ID("rdf_ttl"),

  /**
   * Datastream id of integrated basic search.
   */
  SOLR_DATASTREAM_ID("search_json");


  public final String name;

  GAMSAPIntegrationDatastreamId(String name) {
    this.name = name;
  }

}
