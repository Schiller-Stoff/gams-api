package org.zim.gamsapi.TestUtilities;

import org.springframework.core.io.ClassPathResource;
import org.zim.gamsapi.application.Ingest.utils.Bagit.BagInfo;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.time.*;
import java.util.Date;

/**
 * Utility class for loading test bag files.
 */
public class TestBag {

  /**
   * Location of the test bag folder in the resources' folder.
   * Not meant to be used outside.
   */
  private static final String FOLDER_LOCATION = "testfiles/ingest/test-bag";

  private TestBag() {
    throw new IllegalStateException("Utility class");
  }

  public static File loadFile() throws IOException {
    return new ClassPathResource(FOLDER_LOCATION).getFile();
  }

  public static class TestBagInfo {
    public static final String BAGGING_DATE = "2025-08-19";
    public static final String PAYLOAD_OXUM = "27387.6";
    public static final String CONTACT_EMAIL = "dh@uni-graz.at";
    public static final String EXTERNAL_DESCRIPTION = "Test bag for gamsapi tests";

    public static BagInfo generate(){
      return BagInfo.builder()
          .payloadOxum(PAYLOAD_OXUM)
          .date(BAGGING_DATE)
          .contactMail(CONTACT_EMAIL)
          .externalDescription(EXTERNAL_DESCRIPTION)
          .build();
    }

  }

  public static class BagitTxt {
    public static final String BAGIT_VERSION = "1.0";
    public static final String TAG_FILE_CHARACTER_ENCODING = "UTF-8";
  }

  public static class TestBagSipJson {
    public static final String REC_ID = TestDigitalObject.DIGITAL_OBJECT_ID.getValue();
    public static final String TITLE = TestDigitalObject.DIGITAL_OBJECT_TITLE.getValue();
    public static final String DESCRIPTION = TestDigitalObject.DIGITAL_OBJECT_DESCRIPTION.getValue();
    public static final String CREATOR = TestDigitalObject.DIGITAL_OBJECT_CREATOR.getValue();
    public static final String RIGHTS = TestDigitalObject.DIGITAL_OBJECT_RIGHTS.getValue();
    public static final String PUBLISHER = TestDigitalObject.DIGITAL_OBJECT_PUBLISHER.getValue();
    public static final String MAIN_RESOURCE = TestDigitalObject.DIGITAL_OBJECT_MAIN_RESOURCE.getValue();
    public static final String SCHEMA = "https://gitlab.cern.ch/digitalmemory/sip-spec/-/blob/master/sip-schema-d1.json";
    public static final String CREATED_BY = "Pyrilo";
    public static final String SOURCE = "local";
    public static final String PROJECT = TestProject.PROJECT_ABBR.getValue();
    public static final String OBJECT_TYPE = TestDigitalObject.DIGITAL_OBJECT_TYPE.getValue();
    public static final String FUNDER = TestDigitalObject.DIGITAL_OBJECT_FUNDER.getValue();
  }

}
