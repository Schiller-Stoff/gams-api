package org.ddh.gamsapi.domain.Datastream.utils.interfaces;

/**
 * Lightweight projection for Solr fulltext indexing workflow.
 * Returns only the minimal data needed to identify and process datastreams.
 */
public interface IDatastreamIndexingView {

  /**
   * Datastream identifier (e.g., "FULLTEXT_INDEX.json")
   */
  String getDsid();

  /**
   * Parent digital object containing this datastream.
   * Nested projection to access the digital object's ID.
   */
  DigitalObjectIdView getDigitalObject();

  /**
   * Nested projection interface to access only the digital object ID.
   */
  interface DigitalObjectIdView {
    String getId();
  }
}