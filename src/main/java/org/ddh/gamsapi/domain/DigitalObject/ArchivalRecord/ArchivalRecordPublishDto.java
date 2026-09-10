package org.ddh.gamsapi.domain.DigitalObject.ArchivalRecord;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.Instant;

/**
 * DTO for publishing an archival record
 */
@Data
public class ArchivalRecordPublishDto {

  @NotNull
  private Instant publicationTimeStamp;

}
