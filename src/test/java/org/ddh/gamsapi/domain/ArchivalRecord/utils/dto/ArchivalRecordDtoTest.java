package org.ddh.gamsapi.domain.ArchivalRecord.utils.dto;

import org.assertj.core.api.Assertions;
import org.ddh.gamsapi.TestUtilities.TestArchivalRecord;
import org.ddh.gamsapi.UnitTest;
import org.ddh.gamsapi.domain.ArchivalRecord.utils.ArchivalState;
import org.ddh.gamsapi.domain.ArchivalRecord.utils.interfaces.ArchivalRecordCompactView;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

class ArchivalRecordDtoTest extends UnitTest {


  @Nested
  class From {

    @Test
    void convertedObjectContainsExpectedValues(){
      ArchivalRecordCompactView archivalRecordCompactView = new ArchivalRecordCompactViewImpl();

      var converted = ArchivalRecordDto.from(archivalRecordCompactView);

      Assertions.assertThat(converted).isNotNull();

      Assertions.assertThat(converted.getPid()).isEqualTo(archivalRecordCompactView.getPid());
      Assertions.assertThat(converted.getObjectId()).isEqualTo(archivalRecordCompactView.getDigitalObjectId());
      Assertions.assertThat(converted.getArchivalState()).isEqualTo(archivalRecordCompactView.getArchivalState());
      Assertions.assertThat(converted.getExternalId()).isEqualTo(archivalRecordCompactView.getExternalId());

      Assertions.assertThat(converted.getPublicationTimeStamp()).isEqualTo(archivalRecordCompactView.getPublicationTimeStamp());
      Assertions.assertThat(converted.getCreated()).isEqualTo(archivalRecordCompactView.getCreated());

    }

  }

  static class ArchivalRecordCompactViewImpl implements ArchivalRecordCompactView {

    Instant created = Instant.now();
    Instant publicationTimeStamp = Instant.now().plus(1, ChronoUnit.HOURS);

    @Override
    public String getPid() {
      return TestArchivalRecord.PID;
    }

    @Override
    public String getDigitalObjectId() {
      return TestArchivalRecord.DIGITAL_OBJECT_ID;
    }

    @Override
    public ArchivalState getArchivalState() {
      return TestArchivalRecord.ARCHIVAL_STATE;
    }

    @Override
    public String getExternalId() {
      return TestArchivalRecord.EXTERNAL_ID;
    }

    @Override
    public Instant getCreated() {
      return created;
    }

    @Override
    public Instant getPublicationTimeStamp() {
      return publicationTimeStamp;
    }
  }


}
