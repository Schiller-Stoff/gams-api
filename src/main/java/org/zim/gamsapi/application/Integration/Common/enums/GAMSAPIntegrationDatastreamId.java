package org.zim.gamsapi.application.Integration.Common.enums;

/**
 * Contains specific datastream ids for gams integration.
 */
public enum GAMSAPIntegrationDatastreamId {

  /**
   * Datastream id of the dublin core datastream.
   */
  DUBLIN_CORE_DATASTREAM_ID("DC.xml"),

  /**
   * Datastream id of the source datastream.
   */
  SOURCE_DATASTREAM_ID("SOURCE"),

  /**
   * Datastream id of defined rdf datastream.
   */
  RDF_DATASTREAM_ID("RDF.xml"),

  /**
   * Datastream id of integrated basic search.
   */
  SEARCH_DATASTREAM_ID("SEARCH.json");


  public final String name;

  GAMSAPIntegrationDatastreamId(String name) {
    this.name = name;
  }

}
