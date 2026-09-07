package org.ddh.gamsapi.domain.DigitalObject.ArchivalRecord;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.time.Instant;

/**
 * DTO for creating an ArchivalRecord (represents user requests)
 */
@Data
public class ArchivalRecordCreateDto {
  /**
   * This might be empty because digitalObjectId must be defined by the endpoint
   */
  private String digitalObjectId;
  @NotEmpty
  private String pid;
  @NotEmpty
  private Instant timeStamp;
  @NotEmpty
  private String externalId;
  @NotEmpty
  private ArchivingStatus archivingStatus;
}
