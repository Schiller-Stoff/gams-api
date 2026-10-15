package org.ddh.gamsapi.domain.ArchivalRecord.utils.dto;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.ddh.gamsapi.domain.ArchivalRecord.utils.ArchivalState;

/**
 * DTO for changing an archival record
 */
@Data
public class ArchivalRecordUpdateDto {
  @Nullable
  private String objectId;
  @Nullable
  private String publicationTimeStamp;
  @Nullable
  private String externalId;
  @NotNull
  private ArchivalState archivalState;
}
