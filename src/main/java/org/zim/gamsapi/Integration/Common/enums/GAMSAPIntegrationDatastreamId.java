package org.zim.gamsapi.Integration.Common.enums;

/**
 * Contains specific datastream ids for gams integration.
 */
public enum GAMSAPIntegrationDatastreamId {

  /**
   * Datastream id of the source datastream.
   */
  SOURCE_DATASTREAM_ID("SOURCE"),

  /**
   * Datastream id of defined rdf datastream.
   */
  RDF_DATASTREAM_ID("RDF"),

  /**
   * Datastream id of integrated basic search.
   */
  SEARCH_DATASTREAM_ID("SEARCH_INDEX");


  public final String name;

  GAMSAPIntegrationDatastreamId(String name) {
    this.name = name;
  }

}
