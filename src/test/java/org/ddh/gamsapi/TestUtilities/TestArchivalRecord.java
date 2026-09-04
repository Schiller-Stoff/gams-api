package org.ddh.gamsapi.TestUtilities;

import org.ddh.gamsapi.domain.DigitalObject.ArchivalRecord.ArchivalRecord;
import org.ddh.gamsapi.domain.DigitalObject.ArchivalRecord.ArchivingStatus;
import org.ddh.gamsapi.domain.DigitalObject.DigitalObject;

import java.time.Instant;

public class TestArchivalRecord {
  public static final String DIGITAL_OBJECT_ID = TestDigitalObject.DIGITAL_OBJECT_ID.getValue();
  public static final String PID = "10.5281/zenodo.17178206";
  public static final Instant TIME_STAMP = Instant.now();
  public static final ArchivingStatus ARCHIVING_STATUS = ArchivingStatus.DRAFTED;
  public static final String EXTERNAL_ID = "22299576";

  public static ArchivalRecord generate(DigitalObject digitalObject){
    var archivalRecord = new ArchivalRecord();
    archivalRecord.setDigitalObject(digitalObject);
    archivalRecord.setPid(PID);
    archivalRecord.setTimeStamp(TIME_STAMP);
    archivalRecord.setArchivingStatus(ARCHIVING_STATUS);
    archivalRecord.setExternalId(EXTERNAL_ID);
    return archivalRecord;
  }

}
