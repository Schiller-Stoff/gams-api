package org.ddh.gamsapi.domain.ArchivalRecord;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.ddh.gamsapi.domain.ArchivalRecord.utils.ArchivalState;

/**
 * DTO for changing an archival record
 */
@Data
public class ArchivalRecordUpdateDto {
  private String objectId;
  private String publicationTimeStamp;
  private String externalId;

  @NotNull
  private ArchivalState archivalState;
}
