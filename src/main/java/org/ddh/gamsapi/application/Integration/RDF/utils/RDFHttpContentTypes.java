package org.ddh.gamsapi.application.Integration.RDF.utils;

/**
 * Container for all content types related to rdf e.g. text/n-quads etc.
 */
public enum RDFHttpContentTypes {

  /**
   * Content type for rdf quad statements.
   */
  TEXT_N_QUADS("text/n-quads");

  public final String name;

  RDFHttpContentTypes(String name){
    this.name = name;
  }

}
