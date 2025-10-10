package org.zim.gamsapi.application.Ingest.utils;


/**
 * Contains static values for the Ingest module, like form part names.
 */
public enum IngestStatics {

  /**
   * The name of the form part that contains the zipped BagIt folder.

   */
  FORM_PART_NAME("subInfoPackZIP");

  public final String name;
  IngestStatics(String name){
    this.name = name;
  }


}
