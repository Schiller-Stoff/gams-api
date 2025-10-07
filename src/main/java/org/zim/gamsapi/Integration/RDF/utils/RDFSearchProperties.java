package org.zim.gamsapi.Integration.RDF.utils;


/**
 * Container for static types / properties used for GAMS configured triplestores.
 * Like for pid, project-abbreviation etc.
 */
public enum RDFSearchProperties {

  /**
   * Base URI / root of GAMS triples. WITHOUT trailing slash.
   */
  GAMS_BASE_URL("https://gams.uni-graz.at"),

  /**
   * GAMS predicate defining the pid of a triple (in sense of the sourcePID)
   */
  HAS_ID("<https://gams.uni-graz.at/ontology#hasId>"),

  /**
   * GAMS predicate defining GAMS project origin - e.g. like from DERLA project
   */
  HAS_PROJECT_ABBR("<https://gams.uni-graz.at/ontology#hasProjectAbbr>"),

  /**
   * GAMS predicate indicating connected datastream-ids.
   */
  HAS_DATASTREAM("<https://gams.uni-graz.at/ontology#hasDatastream>");

  public final String name;

  RDFSearchProperties(String name){
    this.name = name;
  }

}
