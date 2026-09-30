package org.ddh.gamsapi.domain.ArchivalRecord.utils.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.ddh.gamsapi.domain.ArchivalRecord.ValidPid;
import org.ddh.gamsapi.domain.ArchivalRecord.utils.ArchivalState;

import java.time.Instant;

/**
 * DTO for creating an ArchivalRecord (represents user requests)
 */
@Data
public class ArchivalRecordDto {
  @ValidPid
  private String pid;

  private String objectId;

  private Instant publicationTimeStamp;

  private String externalId;

  @NotNull
  private ArchivalState archivalState;

}
