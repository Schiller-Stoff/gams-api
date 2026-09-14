package org.ddh.gamsapi.domain.ArchivalRecord;

import lombok.Data;

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
}
