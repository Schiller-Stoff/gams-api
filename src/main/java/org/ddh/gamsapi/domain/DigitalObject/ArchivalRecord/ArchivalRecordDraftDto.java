package org.ddh.gamsapi.domain.DigitalObject.ArchivalRecord;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.time.Instant;

/**
 * DTO for achieving the draft state of an archival record.
 */
@Data
public class ArchivalRecordDraftDto {
  @NotEmpty
  private String pid;
  @NotEmpty
  private String externalId;
  @NotEmpty
  private Instant timeStamp;
}
