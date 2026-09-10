package org.ddh.gamsapi.domain.DigitalObject.ArchivalRecord;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.Instant;

/**
 * DTO for achieving the draft state of an archival record.
 */
@Data
public class ArchivalRecordDraftDto {
  @NotEmpty
  private String externalId;
  @NotNull
  private Instant timeStamp;
}
