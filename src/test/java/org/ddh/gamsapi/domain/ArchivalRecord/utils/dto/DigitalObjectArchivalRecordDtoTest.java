package org.ddh.gamsapi.domain.ArchivalRecord.utils.dto;

import org.assertj.core.api.Assertions;
import org.ddh.gamsapi.TestUtilities.TestArchivalRecord;
import org.ddh.gamsapi.TestUtilities.TestDigitalObject;
import org.ddh.gamsapi.UnitTest;
import org.ddh.gamsapi.domain.ArchivalRecord.utils.ArchivalState;
import org.ddh.gamsapi.domain.ArchivalRecord.utils.ArchivalSyncState;
import org.ddh.gamsapi.domain.ArchivalRecord.utils.interfaces.ArchivalRecordCompactView;
import org.ddh.gamsapi.domain.ArchivalRecord.utils.interfaces.DigitalObjectArchivalBaseView;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

class DigitalObjectArchivalRecordDtoTest extends UnitTest {

  @Nested
  class From {

    @Test
    void convertsObjectToExpectedValues(){

      ArchivalRecordCompactView TEST_ARCHIVAL_RECORD_COMPACT_VIEW = new ArchivalRecordCompactViewImpl();
      DigitalObjectArchivalBaseView TEST_DIGITAL_OBJECT_ARCHIVAL_BASE_VIEW = new DigitalObjectArchivalBaseViewImpl();

      var result = DigitalObjectArchivalRecordDto.from(
          TEST_DIGITAL_OBJECT_ARCHIVAL_BASE_VIEW,
          List.of(TEST_ARCHIVAL_RECORD_COMPACT_VIEW)
      );

      // first assert calculated properties according to test data
      Assertions.assertThat(result.archivalSyncState())
          .isEqualTo(ArchivalSyncState.ARCHIVING_IN_PROGRESS);
      Assertions.assertThat(result.needsArchiving()).isTrue();

      Assertions.assertThat(result.modified())
          .isEqualTo(TEST_DIGITAL_OBJECT_ARCHIVAL_BASE_VIEW.getModified());
      Assertions.assertThat(result.objectId()).isEqualTo(TEST_DIGITAL_OBJECT_ARCHIVAL_BASE_VIEW.getObjectId());
      Assertions.assertThat(result.projectAbbr()).isEqualTo(TEST_DIGITAL_OBJECT_ARCHIVAL_BASE_VIEW.getProjectAbbr());

      var resultRecord = result.archivalRecords().getFirst();

      // id assertions
      Assertions.assertThat(resultRecord.getPid()).isEqualTo(TEST_ARCHIVAL_RECORD_COMPACT_VIEW.getPid());
      Assertions.assertThat(resultRecord.getObjectId()).isEqualTo(TEST_ARCHIVAL_RECORD_COMPACT_VIEW.getDigitalObjectId());

      // archiving
      Assertions.assertThat(resultRecord.getExternalId()).isEqualTo(TEST_ARCHIVAL_RECORD_COMPACT_VIEW.getExternalId());
      Assertions.assertThat(resultRecord.getArchivalState()).isEqualTo(TEST_ARCHIVAL_RECORD_COMPACT_VIEW.getArchivalState());

      // time assertions
      Assertions.assertThat(resultRecord.getCreated()).isEqualTo(TEST_ARCHIVAL_RECORD_COMPACT_VIEW.getCreated());
      Assertions.assertThat(resultRecord.getPublicationTimeStamp()).isEqualTo(TEST_ARCHIVAL_RECORD_COMPACT_VIEW.getPublicationTimeStamp());


    }

  }

  @Nested
  class ComputeSyncState {

    @Test
    void computesExpectedSyncState(){

      ArchivalRecordCompactView TEST_ARCHIVAL_RECORD_COMPACT_VIEW = new ArchivalRecordCompactViewImpl();
      DigitalObjectArchivalBaseView TEST_DIGITAL_OBJECT_ARCHIVAL_BASE_VIEW = new DigitalObjectArchivalBaseViewImpl();

      var result = DigitalObjectArchivalRecordDto.from(
          TEST_DIGITAL_OBJECT_ARCHIVAL_BASE_VIEW,
          List.of(TEST_ARCHIVAL_RECORD_COMPACT_VIEW)
      );

      Assertions.assertThat(result.archivalSyncState())
          .isEqualTo(ArchivalSyncState.ARCHIVING_IN_PROGRESS);
      Assertions.assertThat(result.needsArchiving()).isTrue();

    }

  }


  class ArchivalRecordCompactViewImpl implements ArchivalRecordCompactView {

    Instant created = Instant.now();
    Instant modified = Instant.now();

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
      return modified;
    }
  }


  class DigitalObjectArchivalBaseViewImpl implements DigitalObjectArchivalBaseView {

    final Instant modified = Instant.now();

    @Override
    public String getObjectId() {
      return TestDigitalObject.DIGITAL_OBJECT_ID.getValue();
    }

    @Override
    public String getProjectAbbr() {
      return TestDigitalObject.DIGITAL_OBJECT_PROJECT_ABBR.getValue();
    }

    @Override
    public Instant getModified() {
      return modified;
    }
  }

}
