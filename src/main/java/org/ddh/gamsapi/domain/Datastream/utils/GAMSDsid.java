package org.ddh.gamsapi.domain.Datastream.utils;

/**
 * Enum for test datastream.
 * Provides helpers for the generations of test datastreams.
 */
public enum GAMSDsid {

  DC("DC.xml");

  private final String value;

  GAMSDsid(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }


}
