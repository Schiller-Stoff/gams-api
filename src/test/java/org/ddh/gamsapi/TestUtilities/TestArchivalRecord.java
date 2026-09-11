package org.ddh.gamsapi.TestUtilities;

import org.ddh.gamsapi.domain.ArchivalRecord.ArchivalRecord;
import org.ddh.gamsapi.domain.DigitalObject.DigitalObject;

public class TestArchivalRecord {
  public static final String DIGITAL_OBJECT_ID = TestDigitalObject.DIGITAL_OBJECT_ID.getValue();
  public static final String PID = "hdl:11471/518.10.1.4714";
  public static final String EXTERNAL_ID = "22299576";

  public static ArchivalRecord generate(DigitalObject digitalObject){
    var archivalRecord = new ArchivalRecord();
    archivalRecord.setDigitalObject(digitalObject);
    archivalRecord.setPid(PID);
    archivalRecord.setExternalId(EXTERNAL_ID);
    archivalRecord.setPublicationTimeStamp(null);
    return archivalRecord;
  }

  public static ArchivalRecord generate(
      DigitalObject digitalObject,
      String pid,
      String externalId
      ){

    var archivalRecord = new ArchivalRecord();
    archivalRecord.setDigitalObject(digitalObject);
    archivalRecord.setPid(pid);
    archivalRecord.setPublicationTimeStamp(null);
    archivalRecord.setExternalId(externalId);
    return archivalRecord;

  }

}
