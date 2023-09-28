package org.zim.gamsapi.Integration;

/**
 * Contains specific datastream ids for GAMS CMS.
 */
public enum GamsDatastreamIds {

  SOURCE_DATASTREAM_ID("source_xml"),

  RDF_DATASTREAM_ID("rdf_ttl"),
  SOLR_DATASTREAM_ID("search_json");


  public final String name;

  GamsDatastreamIds(String name) {
    this.name = name;
  }

}
