package org.ddh.gamsapi.domain.DigitalObject.ArchivalRecord;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

/**
 * DTO for achieving the draft state of an archival record.
 */
@Data
public class ArchivalRecordDraftDto {
  @NotEmpty
  private String externalId;
}
