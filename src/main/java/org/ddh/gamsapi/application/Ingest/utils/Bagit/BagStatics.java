package org.ddh.gamsapi.application.Ingest.utils.Bagit;

/**
 * Holds static values related to the bag export process.
 * (e.g. in case a not-ingested bag should be exported)
 */
public class BagStatics {
  private BagStatics() {
    /* This utility class should not be instantiated */
  }

  public static final String BAG_CONTACT_MAIL = "dh@uni-graz.at";
  public static final String BAG_SCHEMA = "https://raw.githubusercontent.com/cerndigitalmemory/sip-spec/refs/heads/develop/sip-schema-d1.json";
  public static final String BAG_VERSION = "1.0";
  public static final String BAG_SOURCE_WEBCLIENT = "data manually uploaded / created via the gams-api webclient";
  public static final String BAG_EXTERNAL_DESCRIPTION = "Export from the gams-api.";
  public static final String TAG_FILE_CHARACTER_ENCODING = "UTF-8";

}
