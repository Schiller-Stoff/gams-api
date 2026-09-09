package org.ddh.gamsapi.domain.DigitalObject.ArchivalRecord;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.time.Instant;

/**
 * DTO for creating an ArchivalRecord (represents user requests)
 */
@Data
public class ArchivalRecordReserveDto {
  /**
   * This might be empty because digitalObjectId must be defined by the endpoint
   */
  private String digitalObjectId; // TODO this should stay outside
  @NotEmpty
  private String pid;
  @NotEmpty
  private Instant timeStamp; // TODO remove handled by the system!
  @NotEmpty
  private String externalId; // TODO remove this cannot be known!
}
