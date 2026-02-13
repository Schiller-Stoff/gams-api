package org.ddh.gamsapi.domain.DigitalObject.utils;

/**
 * States a digital object can be in.
 */
public enum DataOrigin {

  /**
   * Digital object derived it's state from an ingested bag - without additional
   * changes made on server side. It completely corresponds to the bag that was ingested.
   */
  BAG_INGEST,

  /**
   * Digital object derived it's state from manual input using CRUD operations.
   * There was no bag involved in the creation.
   */
  MANUAL;
}
