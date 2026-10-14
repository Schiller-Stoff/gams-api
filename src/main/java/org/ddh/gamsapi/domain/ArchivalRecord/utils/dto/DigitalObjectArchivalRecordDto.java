package org.ddh.gamsapi.domain.ArchivalRecord.utils.dto;

import jakarta.validation.constraints.NotNull;
import org.ddh.gamsapi.domain.ArchivalRecord.utils.interfaces.ArchivalRecordCompactView;
import org.ddh.gamsapi.domain.ArchivalRecord.utils.ArchivalState;
import org.ddh.gamsapi.domain.ArchivalRecord.utils.ArchivalSyncState;
import org.ddh.gamsapi.domain.ArchivalRecord.utils.interfaces.DigitalObjectArchivalBaseView;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public record DigitalObjectArchivalRecordDto(
    @NotNull String objectId,
    @NotNull String projectAbbr,
    @NotNull Instant modified,
    boolean needsArchiving,
    @NotNull ArchivalSyncState archivalSyncState,
    @NotNull List<ArchivalRecordDto> archivalRecords
) {

  // TODO test!
  public static DigitalObjectArchivalRecordDto from(
      DigitalObjectArchivalBaseView digitalObject,
      List<ArchivalRecordCompactView> records
  ) {
    List<ArchivalRecordCompactView> safeRecords = (records == null) ? List.of() : records;

    List<ArchivalRecordDto> archivalRecords = new ArrayList<>();
    for (ArchivalRecordCompactView curRecord : safeRecords) {
      archivalRecords.add(ArchivalRecordDto.from(curRecord));
    }

    ArchivalSyncState syncState = computeSyncState(digitalObject.getModified(), safeRecords);
    boolean needsArchiving = (syncState != ArchivalSyncState.UP_TO_DATE);

    return new DigitalObjectArchivalRecordDto(
        digitalObject.getObjectId(),
        digitalObject.getProjectAbbr(),
        digitalObject.getModified(),
        needsArchiving,
        syncState,
        archivalRecords
    );
  }

  // TODO test
  private static ArchivalSyncState computeSyncState(
      Instant objectModified,
      List<ArchivalRecordCompactView> records
  ) {
    if (records.isEmpty()) {
      return ArchivalSyncState.NEVER_ARCHIVED;
    }

    // 1. Check for any active/blocking record (RESERVED or DRAFT)
    Optional<ArchivalRecordCompactView> activeRecord = records.stream()
        .filter(r -> ArchivalState.getBlockingStatuses().contains(r.getArchivalState()))
        .max(Comparator.comparing(
            ArchivalRecordCompactView::getCreated,
            Comparator.nullsLast(Comparator.naturalOrder())
        ));

    if (activeRecord.isPresent()) {
      Instant draftCreated = activeRecord.get().getCreated();
      if (draftCreated != null && objectModified != null && objectModified.isAfter(draftCreated)) {
        return ArchivalSyncState.ACTIVE_RECORD_OUTDATED;
      }
      return ArchivalSyncState.ARCHIVING_IN_PROGRESS;
    }

    // 2. Check latest PUBLISHED record
    Optional<Instant> latestPublicationTimestamp = records.stream()
        .filter(r -> r.getArchivalState() == ArchivalState.PUBLISHED)
        .map(ArchivalRecordCompactView::getPublicationTimeStamp)
        .filter(ts -> ts != null)
        .max(Comparator.naturalOrder());

    if (latestPublicationTimestamp.isEmpty()) {
      return ArchivalSyncState.NEVER_ARCHIVED;
    }

    if (objectModified != null && objectModified.isAfter(latestPublicationTimestamp.get())) {
      return ArchivalSyncState.NEEDS_REARCHIVING;
    }

    return ArchivalSyncState.UP_TO_DATE;
  }
}